package org.bmedia.Processing;

/**
 * Class to hold information about an action the ingester should perform
 * <p>
 * TODO: if we're not using delete actions, we can probably restructure or remove this class
 *
 * @param <T>
 */
public class QueueAction<T> {

    // Private variables

    private final T data;
    private final String actionType;

    /**
     * @param data       Data for ingester to act on
     * @param actionType Type of action ingester should perform on this data
     */
    public QueueAction(T data, String actionType) {
        this.data = data;
        this.actionType = actionType;
    }

    /**
     * Gets data
     *
     * @return Data
     */
    public T getData() {
        return data;
    }

    /**
     * Gets the action type
     *
     * @return Action type
     */
    public String getActionType() {
        return actionType;
    }

}
