package i5.bml.parser.types;

public enum BuiltinAnnotation {

    USER_STARTED_CHAT(BMLAnnotationType.MESSENGER),

    USER_SENT_MESSAGE(BMLAnnotationType.MESSENGER),

    USER_JOINED_CHAT(BMLAnnotationType.MESSENGER),

    USER_LEFT_CHAT(BMLAnnotationType.MESSENGER),

    BOT_ADDED(BMLAnnotationType.MESSENGER),

    BOT_REMOVED(BMLAnnotationType.MESSENGER),

    BOT_COMMAND(BMLAnnotationType.MESSENGER),

    ACTION(BMLAnnotationType.ACTION),

    ROUTINE(BMLAnnotationType.ROUTINE),

    USER_POSTED(BMLAnnotationType.SOCIAL),

    USER_WAS_MENTIONED(BMLAnnotationType.SOCIAL),

    TWEET_CONTAINS(BMLAnnotationType.TWITTER),

    TWITTER_USER_RETWEETED(BMLAnnotationType.TWITTER),

    EMAIL_RECEIVED(BMLAnnotationType.EMAIL);

    public final BMLAnnotationType annotationType;

    BuiltinAnnotation(BMLAnnotationType annotationType) {
        this.annotationType = annotationType;
    }
}
