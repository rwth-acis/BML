package i5.bml.parser.types;

import i5.bml.parser.types.annotations.BMLAnnotationType;

public enum BuiltinAnnotation {

    USER_STARTED_CHAT(BMLAnnotationType.MESSENGER, "UserStartedChat"),

    USER_SENT_MESSAGE(BMLAnnotationType.MESSENGER, "UserSentMessage"),

    USER_JOINED_CHAT(BMLAnnotationType.MESSENGER, "UserJoinedChat"),

    USER_LEFT_CHAT(BMLAnnotationType.MESSENGER, "UserLeftChat"),

    BOT_STARTED(BMLAnnotationType.BOT, "BotStarted"),

    BOT_ADDED(BMLAnnotationType.MESSENGER, "BotAdded"),

    BOT_REMOVED(BMLAnnotationType.MESSENGER, "BotRemoved"),

    BOT_COMMAND(BMLAnnotationType.MESSENGER, "BotCommand"),

    ACTION(BMLAnnotationType.ACTION, "Action"),

    ROUTINE(BMLAnnotationType.ROUTINE, "Routine"),

    USER_POSTED(BMLAnnotationType.SOCIAL, "UserPosted"),

    USER_WAS_MENTIONED(BMLAnnotationType.SOCIAL, "UserWasMentioned"),

    TWEET_CONTAINS(BMLAnnotationType.TWITTER, "TweetContains"),

    TWITTER_USER_RETWEETED(BMLAnnotationType.TWITTER, "TwitterUserRetweeted"),

    EMAIL_RECEIVED(BMLAnnotationType.EMAIL, "EmailReceived");

    public final BMLAnnotationType annotationType;

    public final String annotationName;

    BuiltinAnnotation(BMLAnnotationType annotationType, String annotationName) {
        this.annotationType = annotationType;
        this.annotationName = annotationName;
    }

    @Override
    public String toString() {
        return annotationName;
    }
}
