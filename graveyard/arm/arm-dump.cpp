#include "ArmDisassembler.h"

#include <cstdlib>
#include <iostream>

int main(int argc, char* argv[]) {
  ArmDisassembler disassembler(std::cout);
  for (int i = 1; i < argc; ++i) {
    disassembler.disassembleFile(argv[i]);
  }
  return EXIT_SUCCESS;
}
