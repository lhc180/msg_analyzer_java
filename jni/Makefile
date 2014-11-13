CC := g++
HEADER := /usr/lib/jvm/java-6-openjdk/include/

CXXFLAGS := -g -Wall -shared -fpic

SOURCES := com_price_msg_analyzer_SerialPortJni.cpp msg_dumper_syslog.cpp
OBJS := $(SOURCES:.cpp=.o)

OUTPUT := libmsg_dumper_serial.so

build: $(OUTPUT)
	 cp $(OUTPUT) ../lib/

$(OUTPUT): $(OBJS)
	$(CC) $(CXXFLAGS) $^ -o $@

%.o: %.cpp
	$(CC) $(CXXFLAGS) -c $< -I $(HEADER) -o $@

clean:
	rm -f $(OBJS)
	rm $(OUTPUT)

