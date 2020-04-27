package com.dpas.server;

public class ServerDataStore {
    public static final String COMMON_USERS_FILE = "data/users.tmp";
    public static final String COMMON_PARTICULAR_FILE = "data/particular.tmp";
    public static final String COMMON_GENERAL_FILE = "data/general.tmp";
    public static final String COMMON_POSTID_FILE = "data/postid.tmp";
    public static final String MSG_USERS = "Users successfully read/written to file.";
    public static final String MSG_PARTICULAR = "Particular post successfully read/written to file.";
    public static final String MSG_GENERAL = "General post successfully read/written to file.";
    public static final String MSG_POSTID = "Current post ID successfully read/written to file.";
    public static final String MSG_ERROR_CERTIFICATE = "User is not registered in keystore.";
    public static final String MSG_ERROR_GETTOKEN_SIG = "Invalid signature and/or hash. GetToken request denied.";
    public static final String MSG_ERROR_REGISTER_SIG = "Invalid signature and/or hash. Register request denied.";
    public static final String MSG_ERROR_INVALID_REF = "Invalid reference. There is no post with that reference";
    public static final String MSG_ERROR_POST_MSG_LEN = "Invalid message length. Message needs to be smaller than 255 characters.";
    public static final String MSG_ERROR_NOT_REGISTERED = "User is not registered";
    public static final String MSG_ERROR_POST_SIG = "Invalid signature and/or hash. Post request denied.";
    public static final String MSG_ERROR_TOKEN_EXPIRED = "Token has already expired.";
    public static final String MSG_ERROR_POST_GENERAL_SIG = "Invalid signature and/or hash. Post General request denied.";
    public static final String MSG_ERROR_READ_SIG = "Invalid signature and/or hash. Read request denied.";
    public static final String MSG_ERROR_READ_NUMBER = "Invalid message number. Number needs to be positive.";
    public static final String MSG_ERROR_INVALID_KEY = "Invalid key. There is no user with the specified key: ";
    public static final String MSG_ERROR_READ_GENERAL_SIG = "Invalid signature and/or hash. Read General request denied.";
}