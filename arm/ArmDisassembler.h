#ifndef ARM_DISASSEMBLER_H_included
#define ARM_DISASSEMBLER_H_included

#include <stdint.h>
#include <iosfwd>

class ArmDisassembler {
public:
  ArmDisassembler(std::ostream& os);
  
  void disassembleFile(const std::string& path);
  
  void disassembleInstruction(uint32_t address, uint32_t instruction);
  
private:
  std::ostream& os;
};

#endif
