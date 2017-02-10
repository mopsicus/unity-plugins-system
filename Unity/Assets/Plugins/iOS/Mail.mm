//
//  Mail.m
//  Example plugin
//
//  Created by Mopsicus.ru
//

#import <UIKit/UIKit.h>

@interface Mail : UIViewController {
    NSString *object;
    NSString *receiver;
}
@end

@implementation Mail;

#pragma mark plugin functions

// Convert JSON string to NSDictionary
- (NSDictionary *)jsonToDict:(NSString *)json {
    NSError *error;
    NSData *data = [json dataUsingEncoding:NSUTF8StringEncoding];
    return (error) ? NULL : [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:&error];
}

// Convert NSDictionary to JSON string
- (NSString *)dictToJson:(NSDictionary *)dict {
    NSError *error;
    NSData *data = [NSJSONSerialization dataWithJSONObject:dict options:0 error:&error];
    return (error) ? NULL : [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

// Send data in JSON format to Unity
- (void)sendData:(NSString *)data {
    NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
    [dict setValue:[NSStringFromClass([self class]) lowercaseString] forKey:@"name"];
    [dict setValue:data forKey:@"data"];
    NSString *result = [self dictToJson:dict];
    UnitySendMessage([object cStringUsingEncoding:NSUTF8StringEncoding], [receiver cStringUsingEncoding:NSUTF8StringEncoding], [result cStringUsingEncoding:NSUTF8StringEncoding]);
}

// Send error in JSON format to Unity
- (void)sendError:(int)code data:(NSString *)data {
    NSDictionary *error = [NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithInt:code], @"code", data, @"message", nil];
    NSMutableDictionary *dict = [[NSMutableDictionary alloc] init];
    [dict setValue:[NSStringFromClass([self class]) lowercaseString] forKey:@"name"];
    [dict setValue:error forKey:@"error"];
    NSString *result = [self dictToJson:dict];
    UnitySendMessage([object cStringUsingEncoding:NSUTF8StringEncoding], [receiver cStringUsingEncoding:NSUTF8StringEncoding], [result cStringUsingEncoding:NSUTF8StringEncoding]);
}

// Plugin initialize
- (void)initialize:(NSString *)data {
    NSDictionary *params = [self jsonToDict:data];
    object = [params valueForKey:@"object"];
    receiver = [params valueForKey:@"receiver"];
}

#pragma mark user functions

// Plugin test function
- (void)test {
    [self sendData:@"mail operation success"];
    [self sendError:1 data:@"mail operation error"];
}

// Add your own functions
//- (void)another {
//
//}

#pragma mark interface for Unity

static Mail *plugin = [[Mail alloc] init];

// Functions will be called by Unity
// Don't forget to add postfix to function name.
// It's important - functions names must different in all plugins
extern "C" {
    
    void initMail (char *data) {
        [plugin initialize:[NSString stringWithCString:data encoding:NSUTF8StringEncoding]];
    }
    
    void testMail () {
        [plugin test];
    }
    
    extern void	UnitySendMessage(const char *obj, const char *method, const char *msg);
}


@end
