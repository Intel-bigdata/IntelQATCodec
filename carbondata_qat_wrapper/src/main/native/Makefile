ifeq ($(VERBOSE),1)
ECHO := 
else
ECHO := @
endif

# A function to check whether every environment variable in a list is defined.
check = $(foreach var,$(1),$(if $($(var)),,$(error Please set $(var))))

TARGET := libCarbondataQatJNI.so

MODULES :=

# Source files.
SRCS := $(foreach D,$(MODULES),$(wildcard $D/*.c)) CarbondataQatJNI.c

# Include files.
INCLUDES  := $(addprefix -I,$(MODULES)) \
	-I$(JAVA_HOME)/include \
        -I $(QATZIPSRC)/include \
	-I$(JAVA_HOME)/include/linux \
	-I$(QATZIP_SRC)/include \
	-I/opt/intel/QATzip/include/ \
	-I.

LIB_DIRS :=

# Libraries.
LIBS := dl

CPPFLAGS += $(INCLUDES)
CFLAGS ?= -O3
CFLAGS += -Wall -fstack-protector-all -D_FORTIFY_SOURCE=2
CFLAGS += -Wformat -Wformat-security -Werror=format-security
CFLAGS += -fPIC

ifeq ($(DEBUG),1)
CFLAGS += -g
endif

SONAME_FLAGS = -Wl,-soname=$(TARGET)
LDFLAGS += -shared

all: jni $(TARGET)
	@echo "Build native library done"

$(TARGET) : $(SRCS)
	@echo Compiling dynamic library
	$(ECHO)$(CC) $(CPPFLAGS) $(CFLAGS) \
			$(SRCS) \
			$(LDFLAGS) $(SONAME_FLAGS) \
			$(foreach D,$(LIB_DIRS),-L$D) \
			$(foreach L,$(LIBS),-l$L) \
			-o $@
jni:
	@echo Generating jni header
	$(call check,JAVA_HOME)
	$(ECHO)$(JAVA_HOME)/bin/javah -cp ../../../target/classes \
	com.intel.qat.jni.CarbondataQatJNI

clean:
	$(ECHO)rm -rf *.so *.o
	@echo Cleaning completed

.PHONY : all clean
