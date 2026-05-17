package ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class SimpleDocumentListener implements DocumentListener {
    private final Runnable onChange;

    public SimpleDocumentListener(Runnable onChange) {
        this.onChange = onChange;
    }

    @Override
    public void insertUpdate(DocumentEvent event) {
        onChange.run();
    }

    @Override
    public void removeUpdate(DocumentEvent event) {
        onChange.run();
    }

    @Override
    public void changedUpdate(DocumentEvent event) {
        onChange.run();
    }
}
