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

static int indexOf(const char* s, char ch) {
  for (const char* p = s; *p != 0; ++p) {
    if (*p == ch) {
      return (p - s);
    }
  }
  return -1;
}

class ArmAssembler {
public:
  ArmAssembler(const std::string& inPath, const std::string& outPath)
  : inPath_(inPath), outPath_(outPath), lineNumber_(0)
  {
  }
  
  void assembleFile() {
    std::ifstream in(inPath_.c_str());
    if (!in) {
      error("couldn't open: " + errorString());
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
    writeBytes(&code_[0], &code_[code_.size()]);
  }
  
private:
  // If we're not able to resolve a reference to a label in our first pass, we
  // store a Fixup in 'fixups_' and come back later in 'fixForwardBranches'.
  struct Fixup {
    uint32_t address;
    std::string label;
  };
  
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
      parseCondition();
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
      parseCondition();
      parseS();
      trimLeft();
      const int rd = parseRegister();
      expect(',');
      parseRhs();
      instruction_ |= (mnemonic << 21) | (rd << 12);
    } else if (mnemonic == OP_CMN || mnemonic == OP_CMP ||
               mnemonic == OP_TEQ || mnemonic == OP_TST) {
      // (CMN|CMP|TEQ|TST)<cond>P? rn,<rhs>
      parseCondition();
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
      }
      parseCondition();
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
      parseCondition();
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
      parseCondition();
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
      parseCondition();
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
    if (accept(".align", 6)) {
      trimLeft();
      int alignment = parseInt();
      for (int i = (address() % alignment); i > 0; --i) {
        code_.push_back(0);
      }
    } else if (accept(".byte", 5)) {
      trimLeft();
      int byte = parseInt();
      code_.push_back(byte);
    } else {
      error("unknown directive");
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
  void writeBytes(const uint8_t* begin, const uint8_t* end) {
    const char* path(outPath_.c_str());
    int fd = TEMP_FAILURE_RETRY(open(path, O_CREAT | O_TRUNC | O_WRONLY, 0666));
    if (fd == -1) {
      error("couldn't open output file '" + outPath_ + "': " + errorString());
    }
    
    const uint8_t* p = begin;
    while (p != end) {
      int bytesWritten = TEMP_FAILURE_RETRY(write(fd, p, end - p));
      if (bytesWritten == -1) {
        error("write failed: " + errorString());
        // FIXME: this would leak 'fd' except 'error' actually exits. need scoped_fd.
      }
      std::cerr << "bytes written: " << bytesWritten << "\n";
      p += bytesWritten;
    }
    
    if (TEMP_FAILURE_RETRY(close(fd)) == -1) {
      error("couldn't close output file: " + errorString());
    }
  }
  
  enum Mnemonic {
    // FIXME: document that these values are significant to the processor.
    OP_AND = 0x0, OP_EOR = 0x1, OP_SUB = 0x2, OP_RSB = 0x3,
    OP_ADD = 0x4, OP_ADC = 0x5, OP_SBC = 0x6, OP_RSC = 0x7,
    OP_TST = 0x8, OP_TEQ = 0x9, OP_CMP = 0xa, OP_CMN = 0xb,
    OP_ORR = 0xc, OP_MOV = 0xd, OP_BIC = 0xe, OP_MVN = 0xf,
    // FIXME: document that these don't matter, but should be contiguous.
    M_BL, M_B,
    M_MUL, M_MLA,
    M_LDM, M_STM,
    M_LDR, M_STR,
    M_SWI,
    LAST_MNEMONIC
  };
  
  Mnemonic parseMnemonic() {
    static const char* mnemonics[] = {
      "and", "eor", "sub", "rsb", "add", "adc", "sbc", "rsc",
      "tst", "teq", "cmp", "cmn", "orr", "mov", "bic", "mvn",
      "bl", "b", "mul", "mla", "ldm", "stm", "ldr", "str", "swi", 0
    };
    for (int i = 0; i != LAST_MNEMONIC; ++i) {
      if (accept(mnemonics[i], strlen(mnemonics[i]))) {
        return Mnemonic(i);
      }
    }
    error("unknown mnemonic");
  }
  
  void parseCondition() {
    // "al" is the default, and "nv" is deprecated, so we stop when we hit "al".
    static const char* conditions[] = {
      "eq", "ne", "cs", "cc", "mi", "pl", "vs", "vc",
      "hi", "ls", "ge", "lt", "gt", "le", 0, // al, nv.
    };
    int condition = 0;
    for (; conditions[condition]; ++condition) {
      if (accept(conditions[condition], 2)) {
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
        error("register too large!");
      }
      return reg;
    } else if (accept("sp", 2)) {
      return 13;
    } else if (accept("lr", 2)) {
      return 14;
    } else if (accept("pc", 2)) {
      return 15;
    } else {
      error("expected register");
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
      if (accept(shifts[i], 3)) {
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
    if (accept("0x", 2)) {
      base = 16;
      digits = "0123456789abcdef";
    } else if (accept("0o", 2)) {
      base = 8;
      digits = "01234567";
    } else if (accept("0b", 2)) {
      base = 2;
      digits = "01";
    }
    // FIXME: support character literals like 'c', too?
    // FIXME: recognize when we haven't actually parsed any digits!
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
      error("expected '" + std::string(1, ch) + "'");
    }
  }
  
  void trimLeft() {
    while (isspace(*p_)) {
      ++p_;
    }
  }
  
  bool accept(const char* prefix, size_t prefixLength) {
    int charsLeft = prefixLength;
    int i = 0;
    while (--charsLeft >= 0) {
      if (p_[i] != prefix[i++]) {
        return false;
      }
    }
    p_ += prefixLength;
    return true;
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
  
  static std::string errorString() {
    return strerror(errno);
  }
  
  void error(const std::string& msg) __attribute__((noreturn)) {
    std::cerr << inPath_ << ":";
    if (lineNumber_ > 0) {
      const size_t column = (p_ - line_.c_str());
      std::cerr << lineNumber_ << ":" << column << ":";
    }
    std::cerr << " " << msg << std::endl;
    exit(EXIT_FAILURE);
  }
  
  const std::string inPath_;
  const std::string outPath_;
  
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
    const std::string inPath(argv[i]);
    // FIXME: outPath = inPath.replaceAll("\.s$", "\.bin$");
    const std::string outPath("a.out");
    ArmAssembler assembler(inPath, outPath);
    assembler.assembleFile();
  }
  return EXIT_SUCCESS;
}
