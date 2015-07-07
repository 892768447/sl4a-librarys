LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := gif
LOCAL_SRC_FILES := \
	gif.c \
	giflib/dgif_lib.c \
	giflib/gif_hash.c \
	giflib/gifalloc.c \
#LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/giflib

include $(BUILD_SHARED_LIBRARY)