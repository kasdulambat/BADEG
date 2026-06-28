cmake_minimum_required(VERSION 3.22.1)
project("badeg_core")

add_library(native-lib SHARED native-lib.c mongoose.c)
find_library(log-lib log)
target_link_libraries(native-lib ${log-lib})
