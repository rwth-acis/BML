package i5.bml.parser;

public enum EventType {
    /*
eventType : telegramEvent
        | slackEvent
        | rocketChatEvent
        | hangoutsEvent
        | twitterEvent
        | routineEvent
        | emailEvent ;

telegramEvent : 'Telegram' messengerEvent ;

slackEvent : 'Slack' messengerEvent ;

rocketChatEvent : 'RocketChat' messengerEvent ;

hangoutsEvent : 'Hangouts' messengerEvent ;

messengerEvent : 'UserStartedChat'
                 | 'UserSentMessage'
                 | 'UserJoinedChannel'
                 | 'UserLeftChannel'
                 | 'UserIsTyping'
                 | 'BotAddedToChannel' ;

twitterEvent : 'Twitter'
               ('UserTweeted'
                | 'UserWasMentioned'
                | 'UserWasRetweeted'
                | 'TweetContains')
               '(' StringLiteral ')' ;

routineEvent : 'Routine' '(' StringLiteral ')' ; // Semantic rule: String format -> x ms/s/m/h/d/w

emailEvent : 'Email' 'Received' '(' StringLiteral ')' ; // TODO: Email needs: host, mail, password
     */
}
