CC := g++
HEADER := /usr/lib/jvm/java-6-openjdk/include/

CXXFLAGS := -g -Wall -shared -fpic

SOURCES := com_price_msg_analyzer_SerialPortJni.cpp serial_port_jni_mgr.cpp
OBJS := $(SOURCES:.cpp=.o)

OUTPUT := libserial_port.so

build: $(OUTPUT)
	 cp $(OUTPUT) ../lib/

$(OUTPUT): $(OBJS)
	$(CC) $(CXXFLAGS) $^ -o $@

%.o: %.cpp
	$(CC) $(CXXFLAGS) -c $< -I $(HEADER) -o $@

clean:
	rm -f $(OBJS)
	rm $(OUTPUT)

