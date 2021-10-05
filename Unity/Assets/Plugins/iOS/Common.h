#import <Foundation/Foundation.h>

extern void UnitySendMessage(const char *obj, const char *method, const char *msg); // to send data in Unity app
extern UIViewController *UnityGetGLViewController(); // main controller Unity

@interface Common : NSObject
+ (NSDictionary *)jsonToDict:(NSString *)json;

+ (NSString *)dictToJson:(NSDictionary *)dict;

+ (void)sendData:(NSString *)plugin data:(NSString *)data;

+ (void)sendError:(NSString *)plugin code:(NSString *)code;

+ (void)sendError:(NSString *)plugin code:(NSString *)code data:(NSString *)data;
@end
