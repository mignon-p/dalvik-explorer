#include <cerrno>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <map>
#include <string>
#include <vector>

#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>

static bool startsWith(const char* s, const char* prefix, size_t prefixLength) {
  int charsLeft = prefixLength;
  int i = 0;
  while (--charsLeft >= 0) {
    if (s[i] != prefix[i++]) {
      return false;
    }
  }
  return true;
}

static int indexOf(const char* s, char ch) {
  for (const char* p = s; *p != 0; ++p) {
    if (*p == ch) {
      return (p - s);
    }
  }
  return -1;
}

enum Mnemonic {
  // FIXME: document that these values are significant.
  OP_AND = 0x0, OP_EOR = 0x1, OP_SUB = 0x2, OP_RSB = 0x3,
  OP_ADD = 0x4, OP_ADC = 0x5, OP_SBC = 0x6, OP_RSC = 0x7,
  OP_TST = 0x8, OP_TEQ = 0x9, OP_CMP = 0xa, OP_CMN = 0xb,
  OP_ORR = 0xc, OP_MOV = 0xd, OP_BIC = 0xe, OP_MVN = 0xf,
  // FIXME: document that these don't matter.
  M_BL, M_B,
  M_MUL, M_MLA,
  M_LDR, M_STR,
  M_SWI,
};

struct Fixup {
  uint32_t address;
  std::string label;
};

class ArmAssembler {
public:
  ArmAssembler(const char* path) : path_(path), lineNumber_(0) {
  }
  
  void assembleFile() {
    std::ifstream in(path_.c_str());
    if (!in) {
      std::cerr << path_ << ": couldn't open: " << strerror(errno) << "\n";
      exit(EXIT_FAILURE);
    }
    
    while (getline(in, line_)) {
      ++lineNumber_;
      p_ = line_.c_str();
      
      // Handle labels.
      if (isalnum(*p_)) {
        handleLabel();
      } else {
        // Trim leading whitespace.
        trimLeft();
        if (*p_ == 0 || *p_ == '#') {
          // Skip blank or comment lines.
        } else if (*p_ == '.') {
          handleDirective();
        } else {
          handleInstruction();
        }
      }
    }
    
    fixForwardBranches();
    writeBytes("a.out", &code_[0], &code_[code_.size()]);
  }
  
private:
  int address() {
    return code_.size();
  }
  
  void handleLabel() {
    const char* colon = strchr(p_, ':');
    if (colon == 0) {
      error("labels must be terminated with ':'");
    }
    std::string label(p_, colon);
    if (labels_.find(label) != labels_.end()) {
      error("duplicate definitions of label '" + label + "'");
    }
    labels_[label] = address();
    p_ = colon;
    expect(':');
    ensureOnlySpaceOrCommentAtEndOf("label");
  }
  
  void handleInstruction() {
    instruction_ = 0;
    const Mnemonic mnemonic = parseMnemonic();
    if (mnemonic == OP_ADD || mnemonic == OP_ADC || mnemonic == OP_AND ||
        mnemonic == OP_BIC || mnemonic == OP_EOR || mnemonic == OP_ORR ||
        mnemonic == OP_RSB || mnemonic == OP_RSC || mnemonic == OP_SBC ||
        mnemonic == OP_SUB) {
      // (ADD|ADC|AND|BIC|EOR|ORR|RSB|RSC|SBC|SUB)<cond>S? rd,rn,<rhs>
      parseCondition(3);
      parseS();
      trimLeft();
      const int rd = parseRegister();
      expect(',');
      const int rn = parseRegister();
      expect(',');
      parseRhs();
      instruction_ |= (mnemonic << 21) | (rn << 16) | (rd << 12);
    } else if (mnemonic == OP_MOV || mnemonic == OP_MVN) {
      // (MOV|MVN)<cond>S? rd,<rhs>
      parseCondition(3);
      parseS();
      trimLeft();
      const int rd = parseRegister();
      expect(',');
      parseRhs();
      instruction_ |= (mnemonic << 21) | (rd << 12);
    } else if (mnemonic == OP_CMN || mnemonic == OP_CMP ||
               mnemonic == OP_TEQ || mnemonic == OP_TST) {
      // (CMN|CMP|TEQ|TST)<cond>P? rn,<rhs>
      parseCondition(3);
      // FIXME: P?
      trimLeft();
      const int rn = parseRegister();
      expect(',');
      parseRhs();
      instruction_ |= (mnemonic << 21) | (1 << 20) | (rn << 16);
    } else if (mnemonic == M_B || mnemonic == M_BL) {
      // (B|BL)<cond> label
      if (mnemonic == M_BL) {
        instruction_ |= (1 << 24);
        parseCondition(2);
      } else {
        parseCondition(1);
      }
      trimLeft();
      
      const char* labelStart = p_;
      const char* labelEnd = labelStart + 1;
      while (isalnum(*labelEnd)) {
        ++labelEnd;
      }
      p_ = labelEnd;
      
      Fixup fixup;
      fixup.address = address();
      fixup.label = std::string(labelStart, labelEnd);
      
      uint32_t offset = 0;
      if (!resolveLabel(fixup, offset)) {
        fixups_.push_back(fixup);
      }
      
      instruction_ |= (0x5 << 25) | offset;
    } else if (mnemonic == M_MUL || mnemonic == M_MLA) {
      // MUL<cond>S? rd,rm,rs
      // MLA<cond>S? rd,rm,rs,rn
      parseCondition(3);
      parseS();
      trimLeft();
      const int rd = parseRegister();
      expect(',');
      const int rm = parseRegister();
      expect(',');
      const int rs = parseRegister();
      if (rd == rm) {
        error("destination register and first operand register must differ");
      }
      if (rd == 15 || rs == 15 || rm == 15) {
        error("can't multiply using r15");
      }
      if (mnemonic == M_MLA) {
        expect(',');
        const int rn = parseRegister();
        if (rn == 15) {
          error("can't multiply using r15");
        }
        instruction_ |= (1 << 21) | (rn << 12);
      }
      instruction_ |= (rd << 16) | (rs << 8) | (9 << 4) | (rm << 0);
    } else if (mnemonic == M_LDR || mnemonic == M_STR) {
      // (LDR|STR){cond}{B} <dest>,[<base>{,#<imm>}]{!}
      // (LDR|STR){cond}{B} <dest>,[<base>,{+|-}<off>{,<shift>}]{!}
      // (LDR|STR){cond}{B} <dest>,<expression>
      // (LDR|STR){cond}{B} <dest>,[<base>],#<imm>
      // (LDR|STR){cond}{B} <dest>,[<base>],{+|-}<off>{,<shift>}
      
      // xxxx010P UBWLnnnn ddddoooo oooooooo  Immediate form
      // xxxx011P UBWLnnnn ddddcccc ctt0mmmm  Register form
      parseCondition(3);
      if (mnemonic == M_LDR) {
        instruction_ |= (0x2 << 24) | (1 << 20);
      } else {
        instruction_ |= (0x3 << 24) | (0 << 20);
      }
      parseSuffixAndSetBit('b', (1 << 22));
      // FIXME: P == pre-indexed
      // FIXME: U == positive offset
      // FIXME: W == write-back/translate
      trimLeft();
      const int rd = parseRegister();
      instruction_ |= (rd << 12);
      expect(',');
      // FIXME: implement the other addressing modes.
      expect('[');
      const int rn = parseRegister();
      instruction_ |= (rn << 16);
      expect(',');
      const int rm = parseRegister();
      instruction_ |= (rm << 0);
      expect(']');
    } else if (mnemonic == M_SWI) {
      parseCondition(3);
      trimLeft();
      const int comment = parseInt();
      // FIXME: check 'comment' fits in 24 bits.
      instruction_ |= (0xf << 24) | comment;
    } else {
      error("internal error: unimplemented mnemonic");
    }
    
    /*
     * 
     * (LDM|STM){cond}<type1> <base>{!},<registers>{^}
     * (LDM|STM){cond}<type2> <base>{!},<registers>{^}
     * <type1> is F|E A|D
     * <type2> is I|D B|A
     * <base> is a register
     * <registers> is open-brace comma-separated-list close-brace
     */
    
    ensureOnlySpaceOrCommentAtEndOf("instruction");
    
    printf("%4i : 0x%08x : 0x%08x : %s\n",
           lineNumber_, address(), instruction_, line_.c_str());
    
    code_.push_back((instruction_ >> 24) & 0xff);
    code_.push_back((instruction_ >> 16) & 0xff);
    code_.push_back((instruction_ >> 8) & 0xff);
    code_.push_back((instruction_ >> 0) & 0xff);
  }
  
  void handleDirective() {
    if (startsWith(p_, ".align", 6)) {
      p_ += 6;
      trimLeft();
      int alignment = parseInt();
      for (int i = (address() % alignment); i > 0; --i) {
        code_.push_back(0);
      }
    } else if (startsWith(p_, ".byte", 5)) {
      p_ += 5;
      trimLeft();
      int byte = parseInt();
      code_.push_back(byte);
    } else {
      error("directive not understood");
    }
    ensureOnlySpaceOrCommentAtEndOf("directive");
  }
  
  void ensureOnlySpaceOrCommentAtEndOf(const std::string& where) {
    trimLeft();
    if (*p_ != 0 && *p_ != '#') {
      error("junk at end of " + where + ": '" + std::string(p_) + "'");
    }
  }
  
  // FIXME: throw exceptions and use something like InplaceString.
  void writeBytes(const std::string& path, const uint8_t* begin, const uint8_t* end) {
    int fd = TEMP_FAILURE_RETRY(open(path.c_str(), O_CREAT | O_TRUNC | O_WRONLY, 0666));
    if (fd == -1) {
      error("couldn't open output file: " + std::string(strerror(errno)));
    }
    
    const uint8_t* p = begin;
    while (p != end) {
      int bytesWritten = TEMP_FAILURE_RETRY(write(fd, p, end - p));
      if (bytesWritten == -1) {
        error("write failed: " + std::string(strerror(errno)));
        // FIXME: this would leak 'fd' except 'error' actually exits. need scoped_fd.
      }
      std::cerr << "bytes written: " << bytesWritten << "\n";
      p += bytesWritten;
    }
    
    if (TEMP_FAILURE_RETRY(close(fd)) == -1) {
      error("couldn't close output file: " + std::string(strerror(errno)));
    }
  }
  
  Mnemonic parseMnemonic() {
    if (startsWith(p_, "and", 3)) {
      return OP_AND;
    } else if (startsWith(p_, "eor", 3)) {
      return OP_EOR;
    } else if (startsWith(p_, "sub", 3)) {
      return OP_SUB;
    } else if (startsWith(p_, "rsb", 3)) {
      return OP_RSB;
    } else if (startsWith(p_, "add", 3)) {
      return OP_ADD;
    } else if (startsWith(p_, "adc", 3)) {
      return OP_ADC;
    } else if (startsWith(p_, "sbc", 3)) {
      return OP_SBC;
    } else if (startsWith(p_, "rsc", 3)) {
      return OP_RSC;
    } else if (startsWith(p_, "tst", 3)) {
      return OP_TST;
    } else if (startsWith(p_, "teq", 3)) {
      return OP_TEQ;
    } else if (startsWith(p_, "cmp", 3)) {
      return OP_CMP;
    } else if (startsWith(p_, "cmn", 3)) {
      return OP_CMN;
    } else if (startsWith(p_, "orr", 3)) {
      return OP_ORR;
    } else if (startsWith(p_, "mov", 3)) {
      return OP_MOV;
    } else if (startsWith(p_, "bic", 3)) {
      return OP_BIC;
    } else if (startsWith(p_, "mvn", 3)) {
      return OP_MVN;
    } else if (startsWith(p_, "bl", 2)) {
      return M_BL;
    } else if (startsWith(p_, "b", 1)) {
      return M_B;
    } else if (startsWith(p_, "mul", 3)) {
      return M_MUL;
    } else if (startsWith(p_, "mla", 3)) {
      return M_MLA;
    } else if (startsWith(p_, "ldr", 3)) {
      return M_LDR;
    } else if (startsWith(p_, "str", 3)) {
      return M_STR;
    } else if (startsWith(p_, "swi", 3)) {
      return M_SWI;
    } else {
      error("unknown mnemonic");
    }
  }
  
  void parseCondition(int charsToSkip) {
    p_ += charsToSkip;
    // "al" is the default, and "nv" is deprecated, so we stop when we hit "al".
    static const char* conditions[] = {
      "eq", "ne", "cs", "cc", "mi", "pl", "vs", "vc",
      "hi", "ls", "ge", "lt", "gt", "le", 0, // al, nv.
    };
    int condition = 0;
    for (; conditions[condition]; ++condition) {
      if (startsWith(p_, conditions[condition], 2)) {
        p_ += 2;
        break;
      }
    }
    instruction_ |= (condition << 28);
  }
  
  // Handles the "s" suffix by setting the S bit in the instruction word,
  // causing the instruction to alter the condition codes.
  void parseS() {
    parseSuffixAndSetBit('s', (1 << 20));
  }
  
  void parseSuffixAndSetBit(char suffix, int mask) {
    if (*p_ == suffix) {
      instruction_ |= mask;
      ++p_;
    }
  }
  
  // Parses a register name, either a numbered name r0-r15, or one of the
  // special names "sp", "lr", or "pc".
  int parseRegister() {
    if (*p_ == 'r') {
      ++p_;
      // FIXME: it's a bit weird that you can say r0b1101 or r0xf and so on ;-)
      int reg = parseInt();
      if (reg > 15) {
        std::cerr << "register " << reg << " too large!\n";
        exit(EXIT_FAILURE);
      }
      return reg;
    } else if (startsWith(p_, "sp", 2)) {
      p_ += 2;
      return 13;
    } else if (startsWith(p_, "lr", 2)) {
      p_ += 2;
      return 14;
    } else if (startsWith(p_, "pc", 2)) {
      p_ += 2;
      return 15;
    } else {
      std::cerr << "expected register!\n";
      exit(EXIT_FAILURE);
    }
  }
  
  void parseRhs() {
    if (*p_ == 0) {
      error("expected immediate or register");
    } else if (isdigit(*p_)) {
      // Immediate.
      // xxxx001a aaaSnnnn ddddrrrr bbbbbbbb
      instruction_ |= (1 << 25);
      // FIXME: translate to value and shift!
      // FIXME: check representable!
      // FIXME: automatically translate negative MOVs/CMPs?
      instruction_ |= parseInt();
    } else {
      // Register.
      // xxxx000a aaaSnnnn ddddcccc 0tttmmmm
      instruction_ |= parseRegister();
      if (*p_ != ' ') {
        return;
      }
      ++p_;
      if (!parseShift()) {
        error("expected lsl, lsr, asr, or ror");
      }
      if (*p_++ != ' ') {
        error("expected shift constant");
      }
      const int c = parseInt();
      if (c > 0xffff) {
        error("shift constant too large");
      }
      instruction_ |= (c << 8);
    }
  }
  
  bool parseShift() {
    static const char* shifts[] = { "lsl", "lsr", "asr", "ror" };
    for (int i = 0; i < 4; ++i) {
      if (startsWith(p_, shifts[i], 3)) {
        p_ += 3;
        instruction_ |= (i << 5);
        return true;
      }
    }
    return false;
  }
  
  // Parses non-negative integer literals in decimal, hex (starting "0x"),
  // octal (starting "0o"), or binary (starting "0b").
  int parseInt() {
    int base = 10;
    const char* digits = "0123456789";
    if (startsWith(p_, "0x", 2)) {
      p_ += 2;
      base = 16;
      digits = "0123456789abcdef";
    } else if (startsWith(p_, "0o", 2)) {
      p_ += 2;
      base = 8;
      digits = "01234567";
    } else if (startsWith(p_, "0b", 2)) {
      p_ += 2;
      base = 2;
      digits = "01";
    }
    // FIXME: support character literals like 'c', too?
    int result = 0;
    int digit;
    while ((digit = indexOf(digits, *p_)) != -1) {
      result = (result * base) + digit;
      ++p_;
    }
    return result;
  }
  
  void expect(char ch) {
    if (*p_++ != ch) {
      error("expected '" + std::string(ch, 1) + "'");
    }
  }
  
  void trimLeft() {
    while (isspace(*p_)) {
      ++p_;
    }
  }
  
  bool resolveLabel(const Fixup& fixup, uint32_t& offset) {
    if (labels_.find(fixup.label) == labels_.end()) {
      // If this is a forward branch, and we're not yet in the second pass,
      // this isn't necessarily an error, so let the caller decide what to do.
      return false;
    }
    offset = ((labels_[fixup.label] - fixup.address - 8) / 4);
    offset &= 0x00ffffff;
    return true;
  }
  
  void fixForwardBranches() {
    for (size_t i = 0; i < fixups_.size(); ++i) {
      const Fixup& fixup(fixups_[i]);
      uint32_t offset = 0;
      if (!resolveLabel(fixup, offset)) {
        error("undefined label '" + fixup.label + "'"); // FIXME: which line(s) was it referenced on?
      }
      // Patch the already-generated code.
      code_[fixup.address + 1] = (offset >> 16) & 0xff;
      code_[fixup.address + 2] = (offset >> 8) & 0xff;
      code_[fixup.address + 3] = (offset >> 0) & 0xff;
    }
    std::cerr << "fixups resolved: " << fixups_.size() << "\n";
  }
  
  void error(const std::string& msg) __attribute__((noreturn)) {
    std::cerr << path_ << ":" << lineNumber_ << ": " << msg << std::endl;
    exit(EXIT_FAILURE);
  }
  
  std::string path_;
  
  int lineNumber_;
  std::string line_;
  const char* p_;
  
  typedef std::map<std::string, uint32_t> Labels;
  Labels labels_;
  
  std::vector<Fixup> fixups_;
  
  uint32_t instruction_;
  std::vector<uint8_t> code_;
};

int main(int argc, char* argv[]) {
  for (int i = 1; i < argc; ++i) {
    ArmAssembler assembler(argv[i]);
    assembler.assembleFile();
  }
  return EXIT_SUCCESS;
}
