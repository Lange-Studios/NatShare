//
//  Bridge.m
//  NatShare
//
//  Created by Yusuf Olokoba on 8/8/19.
//  Copyright © 2019 Yusuf Olokoba. All rights reserved.
//

#import "NSPayload.h"

void* NSCreateSharePayload (const char* subject, void (*completionHandler) (void*), void* context) {
    id<NSPayload> payload = [NSSharePayload.alloc initWithCompletionHandler:^{ completionHandler(context); }];
    return (__bridge_retained void*)payload;
}

void* NSCreateSavePayload (const char* album, void (*completionHandler) (void*), void* context) {
    NSString* albumStr = album ? [NSString stringWithUTF8String:album] : nil;
    id<NSPayload> payload = [NSSavePayload.alloc initWithAlbum:albumStr andCompletionHandler:^{ completionHandler(context); }];
    return (__bridge_retained void*)payload;
}

void NSDispose (void* payloadPtr) {
    id<NSPayload> payload = (__bridge_transfer id<NSPayload>)payloadPtr;
    [payload dispose];
    payload = nil;
}

void NSAddText (id<NSPayload> payload, const char* text) {
    [payload addText:[NSString stringWithUTF8String:text]];
}

void NSAddImage (id<NSPayload> payload, uint8_t* pixelBuffer, int width, int height) { // INCOMPLETE
    
}

void NSAddMedia (id<NSPayload> payload, const char* uri) {
    [payload addMedia:[NSURL URLWithString:[NSString stringWithUTF8String:uri]]];
}

void NSCommit (id<NSPayload> payload) {
    [payload commit];
}
