package org.bmedia.Processing;

import java.nio.file.StandardWatchEventKinds;

public class QueueAction<T> {


    private final T data;
    private final String actionType;

    public QueueAction(T data, String actionType) {
        this.data = data;
        this.actionType = actionType;
    }

    public T getData() {
        return data;
    }

    public String getActionType() {
        return actionType;
    }

}
