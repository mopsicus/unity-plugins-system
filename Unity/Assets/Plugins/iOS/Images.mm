#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#import <AssetsLibrary/AssetsLibrary.h>
#import <AVFoundation/AVFoundation.h>
#include "Common.h"

static NSString *name = @"images";

@interface Images : UIViewController <UIImagePickerControllerDelegate, UINavigationControllerDelegate>
- (id)init;

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info;

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker;

- (UIImage *)normalizeOrientation:(UIImage *)image;

- (void)saveToLibrary:(NSString *)fileName;

- (void)requestPermission;

- (void)checkAndCapture;

- (void)captureImage;
@end

@implementation Images

- (id)init {
    return [super init];
}

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info {
    UIImage *image = (picker.sourceType == UIImagePickerControllerSourceTypeCamera) ? [info objectForKey:UIImagePickerControllerOriginalImage] : [self normalizeOrientation:[info objectForKey:UIImagePickerControllerOriginalImage]];
    NSString *path = [NSTemporaryDirectory() stringByAppendingPathComponent:@"image.jpg"];
    NSData *data = UIImageJPEGRepresentation(image, 0.9);
    [data writeToFile:path atomically:YES];
    int degree = [self getOrientationAngle:image.imageOrientation];
    NSMutableDictionary *json = [[NSMutableDictionary alloc] init];
    [json setValue:path forKey:@"path"];
    [json setValue:[NSNumber numberWithInt:degree] forKey:@"degree"];
    NSString *result = [Common dictToJson:json];
    [Common sendData:name data:result];
    [UnityGetGLViewController() dismissViewControllerAnimated:YES completion:nil];
}

- (int)getOrientationAngle:(UIImageOrientation)orientation {
    switch (orientation) {
        case UIImageOrientationRight:
            return 90;
        case UIImageOrientationDown:
            return 180;
        case UIImageOrientationLeft:
            return 270;
        default:
            return 0;
    }
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    [UnityGetGLViewController() dismissViewControllerAnimated:YES completion:nil];
}

- (UIImage *)normalizeOrientation:(UIImage *)image {
    if (image.imageOrientation == UIImageOrientationUp) {
        return image;
    }
    UIGraphicsBeginImageContextWithOptions(image.size, NO, image.scale);
    [image drawInRect:(CGRect) {0, 0, image.size}];
    UIImage *normalizedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return normalizedImage;
}

- (void)saveToLibrary:(NSString *)filePath {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSData *imgData = [NSData dataWithContentsOfFile:filePath];
        UIImage *image = [[UIImage alloc] initWithData:imgData];
        UIImageWriteToSavedPhotosAlbum(image, self, @selector(image:didFinishSavingWithError:contextInfo:), NULL);
    });
}

- (void)image:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo: (void *)contextInfo {
    if (error) {
        [Common sendError:name code:@"SAVE_ERROR"];
    } else {
        NSMutableDictionary *json = [[NSMutableDictionary alloc] init];
        [json setValue:@"SAVE_SUCCESS" forKey:@"path"];
        [json setValue:[NSNumber numberWithInt:-1] forKey:@"degree"];
        NSString *result = [Common dictToJson:json];
        [Common sendData:name data:result];
    }
}

- (void)requestPermission {
    [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {
        if (!granted) {
            [Common sendError:name code:@"NO_PERMISSION"];
        } else {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self captureImage];
            });
        }
    }];
}

- (void)checkAndCapture {
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    switch (status) {
        case AVAuthorizationStatusAuthorized:
            [self captureImage];
            break;
        case AVAuthorizationStatusDenied:
        case AVAuthorizationStatusRestricted:
            [Common sendError:name code:@"NO_PERMISSION"];
            break;
        case AVAuthorizationStatusNotDetermined:
            [self requestPermission];
            break;
        default:
            [Common sendError:name code:@"IMAGE_ERROR"];
            break;
    }
}

- (void)captureImage {
    UIImagePickerController *picker = [[UIImagePickerController alloc] init];
    picker.delegate = self;
    picker.sourceType = UIImagePickerControllerSourceTypeCamera;
    picker.allowsEditing = NO;
    picker.showsCameraControls = YES;
    [UnityGetGLViewController() presentViewController:picker animated:YES completion:nil];
}

@end

static Images *images = NULL;

extern "C" {

void imagesPick() {
    if (images == NULL) {
        images = [[Images alloc] init];
    }
    UIImagePickerController *picker = [[UIImagePickerController alloc] init];
    picker.delegate = images;
    if ([UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeSavedPhotosAlbum]) {
        picker.sourceType = UIImagePickerControllerSourceTypeSavedPhotosAlbum;
    } else {
        picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
    }
    picker.allowsEditing = NO;
    [UnityGetGLViewController() presentViewController:picker animated:YES completion:nil];
}


void imagesCapture() {
    if (images == NULL) {
        images = [[Images alloc] init];
    }
    [images checkAndCapture];
}


void imagesSave(const char *filePath) {
    if (images == NULL) {
        images = [[Images alloc] init];
    }
    [images saveToLibrary:[NSString stringWithUTF8String:filePath]];
}

void imagesSettings(){
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:UIApplicationOpenSettingsURLString] options:@{} completionHandler:nil];
}

}
