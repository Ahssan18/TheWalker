package app.thewalker.listeners;

public interface LastLoginTimeStampCallback {
    void lastLogin(long ts,String previous_email);
}
