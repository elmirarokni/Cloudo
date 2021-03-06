package com.promiseek.cloudo;

import android.content.Context;

import org.drinkless.td.libcore.telegram.Client;
import org.drinkless.td.libcore.telegram.TdApi;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.promiseek.cloudo.MainActivity.onAuthorizationStateUpdated;

public class UpdateHandler implements Client.ResultHandler {
    private static final ConcurrentMap<Long, TdApi.User> users = new ConcurrentHashMap<Long, TdApi.User>();
    private static final ConcurrentMap<Long, TdApi.BasicGroup> basicGroups = new ConcurrentHashMap<Long, TdApi.BasicGroup>();
    private static final ConcurrentMap<Long, TdApi.Supergroup> supergroups = new ConcurrentHashMap<Long, TdApi.Supergroup>();
    private static final ConcurrentMap<Integer, TdApi.SecretChat> secretChats = new ConcurrentHashMap<Integer, TdApi.SecretChat>();
    private static final ConcurrentMap<Long, TdApi.Chat> chats = new ConcurrentHashMap<Long, TdApi.Chat>();
    private static final NavigableSet<OrderedChat> mainChatList = new TreeSet<OrderedChat>();
    private static final ConcurrentMap<Long, TdApi.UserFullInfo> usersFullInfo = new ConcurrentHashMap<Long, TdApi.UserFullInfo>();
    private static final ConcurrentMap<Long, TdApi.BasicGroupFullInfo> basicGroupsFullInfo = new ConcurrentHashMap<Long, TdApi.BasicGroupFullInfo>();
    private static final ConcurrentMap<Long, TdApi.SupergroupFullInfo> supergroupsFullInfo = new ConcurrentHashMap<Long, TdApi.SupergroupFullInfo>();
    Context context;
    public UpdateHandler(Context context) {
        this.context = context;
    }

    public static void setChatPositions(TdApi.Chat chat, TdApi.ChatPosition[] positions) {
        synchronized (mainChatList) {
            synchronized (chat) {
                for (TdApi.ChatPosition position : chat.positions) {
                    if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                        boolean isRemoved = mainChatList.remove(new OrderedChat(chat.id, position));
                        assert isRemoved;
                    }
                }

                chat.positions = positions;

                for (TdApi.ChatPosition position : chat.positions) {
                    if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                        boolean isAdded = mainChatList.add(new OrderedChat(chat.id, position));
                        assert isAdded;
                    }
                }
            }
        }
    }

    @Override
    public void onResult(TdApi.Object object) {
        switch (object.getConstructor()) {
            case TdApi.UpdateAuthorizationState.CONSTRUCTOR:
                onAuthorizationStateUpdated(context,((TdApi.UpdateAuthorizationState) object).authorizationState);
                break;

            case TdApi.UpdateUser.CONSTRUCTOR:
                TdApi.UpdateUser updateUser = (TdApi.UpdateUser) object;
                users.put(updateUser.user.id, updateUser.user);
                break;
            case TdApi.UpdateUserStatus.CONSTRUCTOR:  {
                TdApi.UpdateUserStatus updateUserStatus = (TdApi.UpdateUserStatus) object;
                TdApi.User user = users.get(updateUserStatus.userId);
                synchronized (user) {
                    user.status = updateUserStatus.status;
                }
                break;
            }
            case TdApi.UpdateBasicGroup.CONSTRUCTOR:
                TdApi.UpdateBasicGroup updateBasicGroup = (TdApi.UpdateBasicGroup) object;
                basicGroups.put(updateBasicGroup.basicGroup.id, updateBasicGroup.basicGroup);
                break;
            case TdApi.UpdateSupergroup.CONSTRUCTOR:
                TdApi.UpdateSupergroup updateSupergroup = (TdApi.UpdateSupergroup) object;
                supergroups.put(updateSupergroup.supergroup.id, updateSupergroup.supergroup);
                break;
            case TdApi.UpdateSecretChat.CONSTRUCTOR:
                TdApi.UpdateSecretChat updateSecretChat = (TdApi.UpdateSecretChat) object;
                secretChats.put(updateSecretChat.secretChat.id, updateSecretChat.secretChat);
                break;

            case TdApi.UpdateNewChat.CONSTRUCTOR: {
                TdApi.UpdateNewChat updateNewChat = (TdApi.UpdateNewChat) object;
                TdApi.Chat chat = updateNewChat.chat;
                synchronized (chat) {
                    chats.put(chat.id, chat);

                    TdApi.ChatPosition[] positions = chat.positions;
                    chat.positions = new TdApi.ChatPosition[0];
                    setChatPositions(chat, positions);
                }
                break;
            }
            case TdApi.UpdateChatTitle.CONSTRUCTOR: {
                TdApi.UpdateChatTitle updateChat = (TdApi.UpdateChatTitle) object;
                TdApi.Chat chat = chats.get(updateChat.chatId);
                synchronized (chat) {
                    chat.title = updateChat.title;
                }
                break;
            }
            case TdApi.UpdateChatPhoto.CONSTRUCTOR: {
                TdApi.UpdateChatPhoto updateChat = (TdApi.UpdateChatPhoto) object;
                TdApi.Chat chat = chats.get(updateChat.chatId);
                synchronized (chat) {
                    chat.photo = updateChat.photo;
                }
                break;
            }
            case TdApi.UpdateChatLastMessage.CONSTRUCTOR: {
                TdApi.UpdateChatLastMessage updateChat = (TdApi.UpdateChatLastMessage) object;
                TdApi.Chat chat = chats.get(updateChat.chatId);
                synchronized (chat) {
                    chat.lastMessage = updateChat.lastMessage;
                    setChatPositions(chat, updateChat.positions);
                }
                break;
            }
            case TdApi.UpdateChatPosition.CONSTRUCTOR: {
                TdApi.UpdateChatPosition updateChat = (TdApi.UpdateChatPosition) object;
                if (updateChat.position.list.getConstructor() != TdApi.ChatListMain.CONSTRUCTOR) {
                    break;
                }

                TdApi.Chat chat = chats.get(updateChat.chatId);
                synchronized (chat) {
                    int i;
                    for (i = 0; i < chat.positions.length; i++) {
                        if (chat.positions[i].list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                            break;
                        }
                    }
                    TdApi.ChatPosition[] new_positions = new TdApi.ChatPosition[chat.positions.length + (updateChat.position.order == 0 ? 0 : 1) - (i < chat.positions.length ? 1 : 0)];
                    int pos = 0;
                    if (updateChat.position.order != 0) {
                        new_positions[pos++] = updateChat.position;
                    }
                    for (int j = 0; j < chat.positions.length; j++) {
                        if (j != i) {
                            new_positions[pos++] = chat.positions[j];
                        }
                    }
                    assert pos == new_positions.length;

                    setChatPositions(chat, new_positions);
                }
                break;
            }
            case TdApi.UpdateChatReadInbox.CONSTRUCTOR: {
                TdApi.UpdateChatReadInbox updateChat = (TdApi.UpdateChatReadInbox) object;
                TdApi.Chat chat = chats.get(updateChat.chatId);
                synchronized (chat) {
                    chat.lastReadInboxMessageId = updateChat.lastReadInboxMessageId;
                    chat.unreadCount = updateChat.unreadCount;
                }
                break;
            }
            case TdApi.UpdateChatReadOutbox.CONSTRUCTOR: {
                TdApi.UpdateChatReadOutbox updateChat = (TdApi.UpdateChatReadOutbox) object;
                TdApi.Chat chat = chats.get(updateChat.chatId);
                synchronized (chat) {
                    chat.lastReadOutboxMessageId = updateChat.lastReadOutboxMessageId;
                }
                break;
            }
            case TdApi.UpdateChatUnreadMentionCount.CONSTRUCTOR: {
                TdApi.UpdateChatUnreadMentionCount updateChat = (TdApi.UpdateChatUnreadMentionCount) object;
                TdApi.Chat chat = chats.get(updateChat.chatId);
                synchronized (chat) {
                    chat.unreadMentionCount = updateChat.unreadMentionCount;
                }
                break;
            }
            case TdApi.UpdateMessageMentionRead.CONSTRUCTOR: {
                TdApi.UpdateMessageMentionRead updateChat = (TdApi.UpdateMessageMentionRead) object;
                TdApi.Chat chat = chats.get(updateChat.chatId);
                synchronized (chat) {
                    chat.unreadMentionCount = updateChat.unreadMentionCount;
                }
                break;
            }
            case TdApi.UpdateChatReplyMarkup.CONSTRUCTOR: {
                TdApi.UpdateChatReplyMarkup updateChat = (TdApi.UpdateChatReplyMarkup) object;
                TdApi.Chat chat = chats.get(updateChat.chatId);
                synchronized (chat) {
                    chat.replyMarkupMessageId = updateChat.replyMarkupMessageId;
                }
                break;
            }
            case TdApi.UpdateChatDraftMessage.CONSTRUCTOR: {
                TdApi.UpdateChatDraftMessage updateChat = (TdApi.UpdateChatDraftMessage) object;
                TdApi.Chat chat = chats.get(updateChat.chatId);
                synchronized (chat) {
                    chat.draftMessage = updateChat.draftMessage;
                    setChatPositions(chat, updateChat.positions);
                }
                break;
            }
            case TdApi.UpdateChatPermissions.CONSTRUCTOR: {
                TdApi.UpdateChatPermissions update = (TdApi.UpdateChatPermissions) object;
                TdApi.Chat chat = chats.get(update.chatId);
                synchronized (chat) {
                    chat.permissions = update.permissions;
                }
                break;
            }
            case TdApi.UpdateChatNotificationSettings.CONSTRUCTOR: {
                TdApi.UpdateChatNotificationSettings update = (TdApi.UpdateChatNotificationSettings) object;
                TdApi.Chat chat = chats.get(update.chatId);
                synchronized (chat) {
                    chat.notificationSettings = update.notificationSettings;
                }
                break;
            }
            case TdApi.UpdateChatDefaultDisableNotification.CONSTRUCTOR: {
                TdApi.UpdateChatDefaultDisableNotification update = (TdApi.UpdateChatDefaultDisableNotification) object;
                TdApi.Chat chat = chats.get(update.chatId);
                synchronized (chat) {
                    chat.defaultDisableNotification = update.defaultDisableNotification;
                }
                break;
            }
            case TdApi.UpdateChatIsMarkedAsUnread.CONSTRUCTOR: {
                TdApi.UpdateChatIsMarkedAsUnread update = (TdApi.UpdateChatIsMarkedAsUnread) object;
                TdApi.Chat chat = chats.get(update.chatId);
                synchronized (chat) {
                    chat.isMarkedAsUnread = update.isMarkedAsUnread;
                }
                break;
            }
            case TdApi.UpdateChatIsBlocked.CONSTRUCTOR: {
                TdApi.UpdateChatIsBlocked update = (TdApi.UpdateChatIsBlocked) object;
                TdApi.Chat chat = chats.get(update.chatId);
                synchronized (chat) {
                    chat.isBlocked = update.isBlocked;
                }
                break;
            }
            case TdApi.UpdateChatHasScheduledMessages.CONSTRUCTOR: {
                TdApi.UpdateChatHasScheduledMessages update = (TdApi.UpdateChatHasScheduledMessages) object;
                TdApi.Chat chat = chats.get(update.chatId);
                synchronized (chat) {
                    chat.hasScheduledMessages = update.hasScheduledMessages;
                }
                break;
            }

            case TdApi.UpdateUserFullInfo.CONSTRUCTOR:
                TdApi.UpdateUserFullInfo updateUserFullInfo = (TdApi.UpdateUserFullInfo) object;
                usersFullInfo.put(updateUserFullInfo.userId, updateUserFullInfo.userFullInfo);
                break;
            case TdApi.UpdateBasicGroupFullInfo.CONSTRUCTOR:
                TdApi.UpdateBasicGroupFullInfo updateBasicGroupFullInfo = (TdApi.UpdateBasicGroupFullInfo) object;
                basicGroupsFullInfo.put(updateBasicGroupFullInfo.basicGroupId, updateBasicGroupFullInfo.basicGroupFullInfo);
                break;
            case TdApi.UpdateSupergroupFullInfo.CONSTRUCTOR:
                TdApi.UpdateSupergroupFullInfo updateSupergroupFullInfo = (TdApi.UpdateSupergroupFullInfo) object;
                supergroupsFullInfo.put(updateSupergroupFullInfo.supergroupId, updateSupergroupFullInfo.supergroupFullInfo);
                break;
            default:
                // print("Unsupported update:" + newLine + object);
        }
    }
}
