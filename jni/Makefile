OS := $(shell uname -s)

ifeq ($(OS),Linux)
JNI_PLATFORM := linux
LIB_NAME := libsqlvalidator.so
else
JNI_PLATFORM := win32
LIB_NAME := sqlvalidator.dll
endif

CFLAGS += -fPIC -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/$(JNI_PLATFORM)" -I"$(CUBRID)/include"
LDFLAGS += -shared

HEADER = com_cubrid_sqlanalyzer_core_runner_QueryParser.h

# $@ matches the target, $< matches the first dependency
sqlvalidator.o : sqlvalidator.c $(HEADER)
	gcc -c $(CFLAGS) -o $@ $<

# $@ matches the target, $< matches the first dependency
$(LIB_NAME) : sqlvalidator.o
	gcc $(LDFLAGS) -o $@ $<
	
all : clean $(LIB_NAME)

clean :
	rm -f sqlvalidator.o
	rm -f sqlvalidator.dll
	rm -f libsqlvalidator.so
