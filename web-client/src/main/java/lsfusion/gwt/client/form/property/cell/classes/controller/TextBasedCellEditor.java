package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.view.CopyPasteUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.popup.PopupMenuItemValue;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.GCompletionType;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.SuggestBox;
import lsfusion.gwt.client.form.property.cell.classes.view.TextBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.view.MainFrame;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static com.google.gwt.user.client.Event.ONPASTE;
import static lsfusion.gwt.client.base.GwtClientUtils.*;
import static lsfusion.gwt.client.form.filter.user.GCompare.CONTAINS;

// now it's a sort of mix of RequestKeepValueCellEditor and RequestReplaceValueCellEditor (depending on needReplace)
public abstract class TextBasedCellEditor extends InputBasedCellEditor {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final TextBoxImpl textBoxImpl = GWT.create(TextBoxImpl.class);

    protected final EditContext editContext;

    private boolean hasList;
    protected GCompletionType completionType;
    private GInputListAction[] actions;
    protected GCompare compare;
    protected SuggestBox suggestBox = null;

    protected boolean isNative() {
        return inputElementType.hasNativePopup();
    }

    public TextBasedCellEditor(EditManager editManager, GPropertyDraw property) {
        this(editManager, property, null, null);
    }
    
    public TextBasedCellEditor(EditManager editManager, GPropertyDraw property, GInputList inputList, GInputListAction[] inputListActions) {
        this(editManager, property, inputList, inputListActions, null);
    }

    public TextBasedCellEditor(EditManager editManager, GPropertyDraw property, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        super(editManager, property);
        this.editContext = editContext;

        if(inputList != null) {
            this.hasList = true;
            this.completionType = inputList.completionType;
            this.actions = inputListActions;
            this.compare = inputList.compare;
        }
    }

    private static String REGEXP_ATTR = "pattern"; //attribute for validity.patternMismatch
    private void updateRegexp(Element element, String regexp) {
        if(regexp != null) {
            element.setAttribute(REGEXP_ATTR, regexp);
        } else {
            element.removeAttribute(REGEXP_ATTR);
        }
    }

    private static String REGEXP_MESSAGE_ATTR = "title"; //attribute for validity.patternMismatch
    private void updateRegexpMessage(Element element, String regexpMessage) {
        if(regexpMessage != null) {
            element.setAttribute(REGEXP_MESSAGE_ATTR, regexpMessage);
        } else {
            element.removeAttribute(REGEXP_MESSAGE_ATTR);
        }
    }

    public static String checkStartEvent(Event event, Element parent, BiFunction<Element, String, Boolean> checkInputValidity) {
        String value = null;
        if (GKeyStroke.isCharDeleteKeyEvent(event)) {
            value = "";
        } else if (GKeyStroke.isCharAddKeyEvent(event)) {
            String input = String.valueOf((char) event.getCharCode());
            value = (checkInputValidity == null || checkInputValidity.apply(parent, input)) ? input : "";
        }
        return value;
    }

    protected boolean started;
    protected String pattern;
    protected JavaScriptObject mask;

    @Override
    public void start(EventHandler handler, Element parent, RenderContext renderContext, boolean notFocusable, PValue oldValue) {

        if(GMouseStroke.isChangeEvent(handler.event)) {
            Integer dialogInputActionIndex = property.getDialogInputActionIndex(actions);
            if (dialogInputActionIndex != null) {
                commitFinish(oldValue, dialogInputActionIndex, CommitReason.FORCED);
                return;
            }
        }
        started = true;

        super.start(handler, parent, renderContext, notFocusable, oldValue);

        if(!isNative()) {
            pattern = renderContext.getPattern();
            mask = getMaskFromPattern();
            if(mask != null) {
                GwtClientUtils.setMask(inputElement, mask);
                inputElement.select(); // setting inputmask somewhy drops selection, later should be solved some other way (positionCaretOnClick doesn't work)
            }
        }

        boolean allSuggestions = true;
        if(needReplace(parent)) {
            boolean selectAll = !GKeyStroke.isChangeAppendKeyEvent(handler.event);

            String value = checkStartEvent(handler.event, parent, this::checkInputValidity);
            if(value != null) {
                allSuggestions = false;
                selectAll = false;
            } else
                value = (property.clearText ? "" : tryFormatInputText(oldValue));

            setTextInputValue(value);

            if (selectAll)
                inputElement.select();
        } // assert !hasOldValue

        if (hasList && !isNative()) {
            suggestBox = createSuggestBox(inputElement, parent);

            // don't update suggestions if editing started with char key event. as editor text is empty on init - request is being sent twice
            // wait for editor key listener to catch the event
            // UPD: for now is reproducible only in panel properties
            if (!GKeyStroke.isCharAddKeyEvent(handler.event)) {
                suggestBox.showSuggestionList(allSuggestions);
            }
        }

        String regexp = renderContext.getRegexp();
        if (regexp != null) {
            updateRegexp(inputElement, regexp);
        }

        String regexpMessage = renderContext.getRegexpMessage();
        if (regexpMessage != null) {
            updateRegexpMessage(inputElement, regexpMessage);
        }
    }

    protected JavaScriptObject getMaskFromPattern() {
        return StringPatternConverter.convert(pattern);
    }

    private boolean hasList() {
//        return hasList;
        return suggestBox != null;
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        if(started) {
            if (hasList()) {
                suggestBox.hideSuggestions();
                suggestBox = null;
            }

            if(mask != null) {
                GwtClientUtils.removeMask(inputElement);
            }

            super.stop(parent, cancel, blurred);
        }
    }

    protected void setTextInputValue(String value) {
        setTextInputValue(inputElement, value);
    }
    public static void setTextInputValue(InputElement element, String value) {
        element.setValue(value);
    }
    private String getTextInputValue() {
        return getTextInputValue(inputElement);
    }

    public static String getTextInputValue(InputElement element) {
        return element.getValue();
    }

    boolean plainPaste = false;
    private void addPasteListener(InputElement inputElement) {
        GwtClientUtils.setEventListener(inputElement, ONPASTE, event -> {
            if (editContext != null && event.getTypeInt() == ONPASTE && !plainPaste) {
                String cbText = CopyPasteUtils.getEventClipboardData(event);
                String modifiedPastedText = (String) editContext.modifyPastedString(cbText);
                if (modifiedPastedText != null && !modifiedPastedText.equals(cbText)) { // to paste via default mechanism otherwise
                    pasteClipboardText(event, modifiedPastedText);
                }
            }
            plainPaste = false;
        });
    }

    protected native boolean pasteClipboardText(Event event, String pastedText)/*-{
        var eventTarget = event.target;
        var cursorPosStart = eventTarget.selectionStart;
        var cursorPosEnd = eventTarget.selectionEnd;
        var v = eventTarget.value;
        var mergedText = v.substring(0, cursorPosStart) + pastedText + v.substring(cursorPosEnd, v.length);
        if (this.@lsfusion.gwt.client.form.property.cell.classes.controller.TextBasedCellEditor::isStringValid(*)(mergedText)) {
            event.stopPropagation();
            event.preventDefault();
            eventTarget.value = mergedText;
            eventTarget.selectionStart = eventTarget.selectionEnd = cursorPosStart + pastedText.length;
            return true;
        }
        return false;
    }-*/;

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        if(hasList() && isThisCellEditor()) {
            suggestBox.onBrowserEvent(handler);
            if(handler.consumed)
                return;
        }

        Event event = handler.event;
        if (InputBasedCellEditor.isInputKeyEvent(event, isMultiLine()) || GMouseStroke.isEvent(event) || GMouseStroke.isContextMenuEvent(event)) {
            boolean isCorrect = true;

            String stringToAdd = null;
            if(GKeyStroke.isCharAddKeyEvent(event))
                stringToAdd = String.valueOf((char) event.getCharCode());
            else if(GKeyStroke.isPasteFromClipboardEvent(event))
                stringToAdd = CopyPasteUtils.getEventClipboardData(event);
            if(stringToAdd != null && !checkInputValidity(parent, stringToAdd))
                isCorrect = false; // this thing is needed to disable inputting incorrect symbols

            handler.consume(isCorrect, false);
        } else if (GKeyStroke.isPlainPasteEvent(event)) {
            plainPaste = true;
        } else {
            Integer inputActionIndex = property.getKeyInputActionIndex(actions, event, true);
            if(inputActionIndex != null) {
                validateAndCommit(parent, inputActionIndex, CommitReason.FORCED);
                return;
            }
        }

        super.onBrowserEvent(parent, handler);
    }

    protected boolean checkInputValidity(Element parent, String stringToAdd) {
        int cursorPosition = textBoxImpl.getCursorPos(inputElement);
        int selectionLength = textBoxImpl.getSelectionLength(inputElement);
        String currentValue = getTextInputValue();
        String firstPart = currentValue == null ? "" : currentValue.substring(0, cursorPosition);
        String secondPart = currentValue == null ? "" : currentValue.substring(cursorPosition + selectionLength);

        return isStringValid(firstPart + stringToAdd + secondPart);
    }

    protected boolean isStringValid(String string) {
        try {
            tryParseInputText(string, false);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, PValue oldValue, Integer renderedWidth, Integer renderedHeight) {
        super.render(cellParent, renderContext, oldValue, renderedWidth, renderedHeight);

        if(renderedHeight != null && isMultiLine())
            // https://stackoverflow.com/questions/7144843/extra-space-under-textarea-differs-along-browsers
            inputElement.getStyle().setVerticalAlign(Style.VerticalAlign.TOP);

        TextBasedCellRenderer.setTextPadding(cellParent); // paddings should be for the element itself (and in general do not change), because the size is set for this element and reducing paddings will lead to changing element size

        addPasteListener(inputElement);
    }

    protected boolean isMultiLine() {
        return false;
    }

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        TextBasedCellRenderer.clearTextPadding(cellParent);

        super.clearRender(cellParent, renderContext, cancel);
    }

    public PValue getCommitValue(Element parent, Integer contextAction) throws InvalidEditException {
        boolean hasList = hasList();
        if (hasList) {
            PopupMenuItemValue selectedItem = suggestBox.selectedItem;
            if (selectedItem != null)
                return selectedItem.getPValue();

            if(contextAction == null && completionType.isOnlyCommitSelection())
                throw new InvalidEditException();
        }

        if (mask != null) {
            if (GwtClientUtils.unmaskedValue(inputElement).isEmpty()) {
                return null;
            } else if (!GwtClientUtils.isCompleteMask(inputElement)) {
                throw new InvalidEditException();
            }
        }

        boolean patternMismatch = isPatternMismatch(inputElement);
        if (patternMismatch) {
            reportValidity(inputElement);
            throw new InvalidEditException(true);
        }

        String stringValue = getTextInputValue();
        if (hasList && contextAction == null && completionType.isCheckCommitInputInList() && !suggestBox.isValidValue(stringValue)) {
            throw new InvalidEditException();
        }
        try {
            return tryParseInputText(stringValue, true); //if button pressed, input element is button
        } catch (ParseException e) {
            throw new InvalidEditException();
        }
    }

    private native void reportValidity(Element element)/*-{
        element.reportValidity();
    }-*/;

    private native boolean isPatternMismatch(Element element)/*-{
        return element.validity.patternMismatch;
    }-*/;

    protected boolean isThisCellEditor() {
//        assert hasList();
        return suggestBox != null;
    }

    private SuggestBox createSuggestBox(InputElement element, Element parent) {
        return new SuggestBox(new SuggestBox.SuggestOracle() {
            private Timer delayTimer;
            private SuggestBox.Request currentRequest; // current pending request
            private SuggestBox.Callback currentCallback;

            private String prevSucceededEmptyQuery;

            @Override
            public void requestSuggestions(SuggestBox.Request request, SuggestBox.Callback callback) {
                currentRequest = request;
                currentCallback = callback;

                if(delayTimer == null)
                    updateAsyncValues();
            }

            private void updateAsyncValues() {
                final SuggestBox.Request request = currentRequest;
                currentRequest = null;
                final SuggestBox.Callback callback = currentCallback;
                currentCallback = null;

                boolean emptyQuery = request.query == null;
                String query = nvl(request.query, "");
//                if(prevSucceededEmptyQuery != null && query.startsWith(prevSucceededEmptyQuery))
//                    return;

                suggestBox.updateDecoration(true);

                //disable selection while loading (delayed)
                Result<Boolean> resultReceived = new Result<>(false);

                // add timer to avoid blinking:
                // popup, when empty popup is followed by non-empty one
                // active row, when there are results updated
                new Timer() {
                    @Override
                    public void run() {
                        if(isThisCellEditor()) {
                            if (emptyQuery && !suggestBox.isSuggestionListShowing()) {
                                callback.onSuggestionsReady(request, new SuggestBox.Response(new ArrayList<>(), false, true));
                                setMinWidth(element, suggestBox, false);
                            }
                            if (!resultReceived.result)
                                suggestBox.clearSelectedItem();
                        }
                    }
                }.schedule(100);

                assert delayTimer == null;
                // we're sending a request, so we want to delay all others for at least 100ms
                // also we're using timer to identify the call in cancelAndFlushDelayed
                Timer execTimer = new Timer() {
                    public void run() {
                        flushDelayed();
                    }
                };
                execTimer.schedule(1000);
                delayTimer = execTimer;

                editManager.getAsyncValues(query, new AsyncCallback<GFormController.GAsyncResult>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        if (isThisCellEditor()) //  && suggestBox.isSuggestionListShowing()
                            cancelAndFlushDelayed(execTimer);
                        resultReceived.set(true);
                    }

                    @Override
                    public void onSuccess(GFormController.GAsyncResult result) {
                        if (isThisCellEditor()) { //  && suggestBox.isSuggestionListShowing() in desktop this check leads to "losing" result, since suggest box can be not shown yet (!), however maybe in web-client it's needed for some reason (but there can be the risk of losing result)
                            suggestBox.setAutoSelectEnabled(completionType.isAutoSelection() && !(isFilterList() && query.contains(MainFrame.matchSearchSeparator)) && !emptyQuery);

                            boolean succeededEmpty = false;
                            if(result.asyncs != null) {
                                List<String> rawSuggestions = new ArrayList<>();
                                ArrayList<PopupMenuItemValue> suggestionList = new ArrayList<>();
                                for (GAsync suggestion : result.asyncs) {
                                    PValue rawValue = PValue.escapeSeparator(suggestion.getRawValue(), compare);
                                    String rawString = PValue.getStringValue(rawValue);
                                    rawSuggestions.add(rawString);
                                    suggestionList.add(new PopupMenuItemValue() {
                                        @Override
                                        public String getDisplayString() {
                                            return PValue.getStringValue(suggestion.getDisplayValue());
                                        }

                                        @Override
                                        public PValue getPValue() {
                                            return rawValue;
                                        }

                                        @Override
                                        public String getReplacementString() {
                                            return rawString;
                                        }
                                    });
                                }
                                suggestBox.setLatestSuggestions(rawSuggestions);
                                callback.onSuggestionsReady(request, new SuggestBox.Response(suggestionList, result.needMoreSymbols, false));
                                setMinWidth(element, suggestBox, true);

                                succeededEmpty = suggestionList.isEmpty();
                            }

                            suggestBox.updateDecoration(result.moreRequests);

                            if(!result.moreRequests && !result.needMoreSymbols) {
                                if (succeededEmpty)
                                    prevSucceededEmptyQuery = query;
                                else
                                    prevSucceededEmptyQuery = null;
                            }

                            cancelAndFlushDelayed(execTimer);
                            resultReceived.set(true);
                        }
                    }
                });
            }

            private void cancelAndFlushDelayed(Timer execTimer) {
                if(delayTimer == execTimer) { // we're canceling only if the current timer has not changed
                    delayTimer.cancel();

                    flushDelayed();
                }
            }

            private void flushDelayed() {
                // assert that delaytimer is equal to execTimer
                delayTimer = null;

                if(currentRequest != null && suggestBox != null) // there was pending request
                    updateAsyncValues();
            }

            private void setMinWidth(InputElement inputElement, SuggestBox suggestBox, boolean offsets) {
                setMinWidth(suggestBox.getPopupElement(), inputElement.getOffsetWidth() - (offsets ? 8 : 0)); //8 = offsets
            }

            private native void setMinWidth(Element element, int minWidth) /*-{
                Array.prototype.forEach.call(element.getElementsByClassName("dropdown-item"), function(item) {
                    item.style.minWidth = minWidth + "px";
                });
            }-*/;
        }, element, parent, completionType, (suggestion, commitReason) -> validateAndCommit(parent, commitReason)) {

            @Override
            protected Widget createButtonsPanel(Element parent) {
//                FlexPanel bottomPanel = new FlexPanel(true);

                FlexPanel buttonsPanel = new FlexPanel();
                buttonsPanel.getElement().addClassName("dropdown-menu-button-panel");

                // block mouse down events to prevent focus issues
                buttonsPanel.addDomHandler(GwtClientUtils::stopPropagation, MouseDownEvent.getType());

                buttonsPanel.add(refreshButton = new SuggestPopupButton(StaticImage.REFRESH_IMAGE_PATH) {
                    @Override
                    public ClickHandler getClickHandler() {
                        return event -> {
                            refreshButtonPressed = true;
                            suggestBox.refreshSuggestionList();
                        };
                    }
                });

                if(actions != null) {
                    for (GInputListAction action : actions) {
                        SuggestPopupButton actionButton = new SuggestPopupButton(action.action) {
                            @Override
                            public ClickHandler getClickHandler() {
                                return event -> validateAndCommit(parent, action.index, CommitReason.FORCED);
                            }
                        };
                        buttonsPanel.add(actionButton);

                        String tooltip = property.getQuickActionTooltipText(action.keyStroke);
                        if (tooltip != null) {
                            TooltipManager.initTooltip(actionButton, new TooltipManager.TooltipHelper() {
                                @Override
                                public String getTooltip(String dynamicTooltip) {
                                    return nvl(dynamicTooltip, tooltip);
                                }
                            });
                        }
                    }
                }

                return buttonsPanel;
            }

            protected Widget createInfoPanel(Element parent) {
                if (isFilterList()) {
                    HTML tip = new HTML(compare == CONTAINS ? messages.suggestBoxContainsTip() : messages.suggestBoxMatchTip(MainFrame.matchSearchSeparator));
                    tip.getElement().addClassName("dropdown-menu-tip");
                    tip.getElement().addClassName("text-secondary");
                    return tip;
                }
                return null;
            }

            @Override
            public void hideSuggestions() { // in theory should be in SuggestOracle, but now it's readonly
                // canceling query
//                    assert isThisCellEditor(); // can be broken when for example tab is changed, it sets display to none before blur occurs
                if (isLoading)
                    editManager.getAsyncValues(null, new AsyncCallback<GFormController.GAsyncResult>() {
                        @Override
                        public void onFailure(Throwable caught) {
                        }

                        @Override
                        public void onSuccess(GFormController.GAsyncResult result) {
                            // assert CANCELED returned
                        }
                    });

                super.hideSuggestions();
            }
        };
    }

    private boolean isFilterList() {
        return compare != null && compare.escapeSeparator();
    }

    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        if(inputText == null || inputText.isEmpty())
            return null;

        if(this instanceof FormatCellEditor) {
            GFormatType formatType = ((FormatCellEditor) this).getFormatType();
            return formatType.parseString(inputText, pattern);
        }
        return PValue.getPValue(inputText);
    }

    protected String tryFormatInputText(PValue value) {
        if(value == null)
            return "";

        if(this instanceof FormatCellEditor) {
            GFormatType formatType = ((FormatCellEditor) this).getFormatType();
            return formatType.formatString(value, pattern);
        }
        return PValue.getStringValue(value);
    }
}
