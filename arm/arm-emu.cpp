#include <cerrno>
#include <cstdlib>
#include <iostream>
#include <string>

#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>

// Almost everything I know about ARM assembler, I learned from
// Peter Cockerell's book "ARM Assembly Language Programming", now available
// on the web here: http://www.peter-cockerell.net/aalp/html/frames.html

static uint32_t r[16]; // 16 registers.
//static bool n_flag, z_flag, v_flag, c_flag; // Condition flags.

static unsigned char memory[32*1024]; // 32KiB of RAM.

enum {
    lr = 14, // r14 is the "link register", lr.
    pc = 15, // r15 is the "program counter", pc.
};

//static std::string uint32_to_string(uint32_t i) {
//    char buf[128];
//    snprintf(buf, sizeof(buf), "0x%08X", i);
//    return buf;
//}

static void dumpRegisters() {
    printf("Registers:\n");
    for (int i = 0; i < 16; ++i) {
        printf(" %2i:0x%08X ", i, r[i]);
        if (((i + 1) % 4) == 0) {
            printf("\n");
        }
    }
}

static void panic(const std::string&, bool) __attribute__((noreturn));
static void panic(const std::string& reason, bool shouldDumpRegisters) {
    printf("Panic: %s\n", reason.c_str());
    if (shouldDumpRegisters) {
        dumpRegisters();
    }
    exit(EXIT_FAILURE);
}

static bool conditionHolds(const uint32_t instruction) {
    // The condition bits are the top four bits of the 32-bit instruction.
    const int conditionBits = (instruction >> 28) & 0xf;
    //std::cout << "condition bits = " << uint32_to_string(conditionBits) << "\n";
    switch (conditionBits) {
    case 0xf: // NV
        return false;
    case 0xe: // AL
        return true;
    case 0x1: // NE
        return (r[pc] & (1 << 30)) == 0;
    case 0x0: // EQ
        return (r[pc] & (1 << 30)) != 0;
    }
    panic("unsupported condition", true);
}

static int fetch(int address) {
    int result = 0;
    result |= memory[address++] << 24;
    result |= memory[address++] << 16;
    result |= memory[address++] << 8;
    result |= memory[address];
    return result;
}

enum BuiltInSwis {
    // RISC OS.
    OS_WriteC = 0x00,  // putchar(r0);
    OS_Write0 = 0x02,  // printf("%s", r0);
    OS_NewLine = 0x03, // putchar('\n');
    OS_Exit = 0x11,    // exit(r2); -- we use r2 because RISC OS did.
    
    // Unix-like. (Unimplemented.)
    Unix_Open,
    Unix_Close,
    Unix_Read,
    Unix_Write,
    Unix_Stat,
    Unix_GetEnv,
    Unix_SetEnv,
    Unix_Exit,
};

static void swi(int comment) {
    if (comment == OS_WriteC) {
        putchar(r[0]);
    } else if (comment == OS_NewLine) {
        putchar('\n');
    } else if (comment == OS_Exit) {
        exit(r[2]);
    } else {
        panic("unimplemented SWI", true);
    }
}

static void readCode(const std::string& path) {
    int fd = TEMP_FAILURE_RETRY(open(path.c_str(), O_RDONLY));
    if (fd == -1) {
        panic("couldn't open output file: " + std::string(strerror(errno)), false);
    }
    
    uint8_t* p = &memory[0];
    const uint8_t* end = &memory[sizeof(memory)];
    while (p != end) {
        int bytesRead = TEMP_FAILURE_RETRY(read(fd, p, end - p));
        std::cerr << "read " << bytesRead << " bytes\n";
        if (bytesRead == -1) {
            panic("read failed: " + std::string(strerror(errno)), false);
            // FIXME: this would leak 'fd' except 'error' actually exits. need scoped_fd.
        }
        if (bytesRead == 0) {
            break;
        }
        p += bytesRead;
    }
    
    if (TEMP_FAILURE_RETRY(close(fd)) == -1) {
        panic("couldn't close file: " + std::string(strerror(errno)), false);
    }
}

int main(int argc, char* argv[]) {
    if (argc != 2) {
        std::cerr << "usage: " << argv[0] << " BINARY\n";
        exit(EXIT_FAILURE);
    }
    std::string path(argv[1]);
    
    // Zero memory.
    memset(memory, 0, sizeof(memory));
    
    // Read code into RAM.
    readCode(path);
    
    // Zero registers.
    for (int i = 0; i < 16; ++i) {
        r[i] = 0;
    }
    
    // Branch to 0.
    // FIXME: set condition/status bits?
    r[pc] = 0;
    
    // Process instructions!
    while (true) {
        //dumpRegisters();
        
        // Fetch.
        const uint32_t instruction = fetch(r[pc] & 0x03ffffff);
        //std::cout << "instruction = " << uint32_to_string(instruction) << "\n";
        r[pc] += 4;
        
        // Skip this instruction if its condition bits require a condition
        // that isn't currently true.
        if (!conditionHolds(instruction)) {
            continue;
        }
        
        if (((instruction >> 26) & 0x3) == 0) {
            // Data processing instruction.
            const bool immediate = (instruction >> 25) & 0x1;
            const int opCode = (instruction >> 21) & 0xf;
            //const bool set_cc = (instruction >> 20) & 0x1;
            const int rn = (instruction >> 16) & 0xf;
            const int rd = (instruction >> 12) & 0xf;
            const int op2 = instruction & 0xfff;
            if (opCode == 0xd) {
                // MOV.
                r[rd] = immediate ? op2 : r[op2];
            } else if (opCode == 0xa) {
                // CMP.
                int result = (r[rn] - op2);
                int newFlags = 0;
                if (result < 0) {
                    newFlags |= (1 << 31);
                } else if (result == 0) {
                    newFlags |= (1 << 30);
                }
                r[pc] = (r[pc] & ~0xf0000000) | newFlags;
            } else if (opCode == 0x4) {
                // ADD.
                r[rd] = r[rn] + op2;
            } else if (opCode == 0x2) {
                // SUB.
                r[rd] = r[rn] - op2;
            } else {
                panic("unknown data processing instruction", true);
            }
        } else if (((instruction >> 25) & 0x7) == 0x5) {
            // Branch.
            const bool link = (instruction >> 24) & 0x1; // FIXME: or use 0b1010 and 0b1011 as separate B/BL cases.
            const uint32_t offset = ((instruction & 0x00ffffff) << 8) >> 8;
            if (link) {
                r[lr] = r[pc] & 0x03ffffff;
            }
            //printf("branching by %i instructions from %x\n", offset, r[pc]);
            r[pc] += offset * 4 + 4;
        } else if (((instruction >> 24) & 0xf) == 0xf) {
            // Software interrupt.
            swi(instruction & 0x00ffffff);
        } else {
            panic("unknown instruction", true);
        }
        
        // Detect exceptional conditions.
        if (r[pc] == 0) {
            panic("branch through zero!", true);
        }
    }
    
    return EXIT_SUCCESS;
}
