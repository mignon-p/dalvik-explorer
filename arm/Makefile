CXXFLAGS=-W -Wall -Werror -O2
LD=$(CXX)

binaries=arm-asm arm-emu arm-dump
objects=arm-asm.o arm-emu.o arm-dump.o ArmDisassembler.o

.PHONY: all
all: $(binaries)

.PHONY: clean
clean:
	rm -f $(binaries) $(objects)

arm-asm: arm-asm.o
	$(CXX) -o $@ $^

arm-emu: arm-emu.o ArmDisassembler.o
	$(CXX) -o $@ $^

arm-dump: arm-dump.o ArmDisassembler.o
	$(CXX) -o $@ $^
