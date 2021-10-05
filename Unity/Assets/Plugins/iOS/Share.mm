#include "Common.h"

static NSString *name = @"share";

@interface Share : UIViewController;

- (id)init;

- (void)text:(NSString *)data;
@end

@implementation Share;

- (id)init {
    return [super init];
}

- (void)text:(NSString *)data {
    NSArray *items = [NSArray arrayWithObjects:data, nil];
    UIActivityViewController *controller = [[UIActivityViewController alloc] initWithActivityItems:items applicationActivities:nil];
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPad) {
        controller.modalPresentationStyle = UIModalPresentationPopover;
        controller.popoverPresentationController.sourceView = self.view;
    } else {
        controller.modalTransitionStyle = UIModalTransitionStyleCoverVertical;
    }
    [UnityGetGLViewController() presentViewController:controller animated:YES completion:nil];
}

static Share *share = NULL;

extern "C" {

void shareText(const char *data) {
    if (share == NULL) {
        share = [[Share alloc] init];
    }
    [share text:[NSString stringWithUTF8String:data]];
}

}

@end
