package platform.gwt.paas.client.pages.project;

import com.gwtplatform.dispatch.shared.DispatchAsync;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.ViewLoader;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.paas.client.Paas;
import platform.gwt.paas.client.common.ErrorHandlingCallback;
import platform.gwt.paas.client.widgets.Toolbar;
import platform.gwt.paas.shared.actions.GetModuleTextAction;
import platform.gwt.paas.shared.actions.GetModuleTextResult;
import platform.gwt.paas.shared.actions.UpdateModulesAction;
import platform.gwt.paas.shared.actions.VoidResult;

public class ModuleEditor extends HLayout {
    private final DispatchAsync dispatcher = Paas.ginjector.getDispatchAsync();

    private Toolbar toolbar;

    private ViewLoader loader;
    private VLayout mainPane;

    private DynamicForm textForm;
    private TextAreaItem textAreaItem;

    private final int moduleId;

    public ModuleEditor(final int moduleId) {
        this.moduleId = moduleId;

        setOverflow(Overflow.HIDDEN);

        loader = new ViewLoader();
        loader.setIcon("loading.gif");
        loader.setLoadingMessage("Загрузка...");

        toolbar = new Toolbar();
        toolbar.addToolStripButton("save.png", "Save", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                saveModule();
            }
        });

        toolbar.addToolStripButton("refresh.png", "Refresh", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                updateModuleText();
            }
        });

        textAreaItem = new TextAreaItem();
        textAreaItem.setShowTitle(false);
        textAreaItem.setColSpan("*");
        textAreaItem.setWidth("*");
        textAreaItem.setHeight("*");

        textForm = new DynamicForm();
        textForm.setWidth100();
        textForm.setHeight100();
        textForm.setFields(textAreaItem);

        mainPane = new VLayout();
        mainPane.addMember(toolbar);
        mainPane.addMember(textForm);

        addMember(mainPane);

        updateModuleText();
    }

    private void saveModule() {
        dispatcher.execute(new UpdateModulesAction(moduleId, textAreaItem.getValueAsString()), new ErrorHandlingCallback<VoidResult>() {
            @Override
            public void success(VoidResult result) {
                SC.say("Saved successfully!");
            }
        });
    }

    public void showLoader() {
        removeMember(mainPane);
        addMember(loader);
    }

    public void hideLoader() {
        removeMember(loader);
        addMember(mainPane);
    }

    public void setModuleText(String text) {
        textAreaItem.setValue(text);
    }

    public void updateModuleText() {
        showLoader();
        dispatcher.execute(new GetModuleTextAction(moduleId), new ErrorHandlingCallback<GetModuleTextResult>() {
            @Override
            public void preProcess() {
                hideLoader();
            }

            @Override
            public void success(GetModuleTextResult result) {
                setModuleText(result.text);
            }
        });
    }

    public String getCurrentText() {
        return textAreaItem.getValueAsString();
    }
}
