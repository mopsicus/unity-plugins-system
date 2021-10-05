#import <StoreKit/StoreKit.h>
#include "Common.h"

static NSString *name = @"review";

@interface Review : UIViewController;

- (id)init;

- (void)open;
@end

@implementation Review;

- (id)init {
    return [super init];
}

- (void)open {
    [SKStoreReviewController requestReview];
}

static Review *review = NULL;

extern "C" {

void reviewOpen() {
    if (review == NULL) {
        review = [[Review alloc] init];
    }
    [review open];
}

bool reviewIsAvailable() {
    float systemVersion = [[[UIDevice currentDevice] systemVersion] floatValue];
    return (systemVersion >= 10.3);
}

}

@end
