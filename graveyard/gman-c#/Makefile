all:
	mkdir -p .generated && \
	gmcs -pkg:gtk-sharp-2.0,gtkhtml-sharp-2.0 -out:.generated/gman.exe *.cs && \
	mono ./.generated/gman.exe
