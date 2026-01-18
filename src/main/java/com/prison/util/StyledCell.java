package com.prison.util;

import javafx.scene.control.TableCell;

public class StyledCell<S, T> extends TableCell<S, T> {

    private final String styleClass;

    public StyledCell(String styleClass) {
        this.styleClass = styleClass;
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            getStyleClass().remove(styleClass);
        } else {
            setText(item.toString());
            if (!getStyleClass().contains(styleClass)) {
                getStyleClass().add(styleClass);
            }
        }
    }
}
