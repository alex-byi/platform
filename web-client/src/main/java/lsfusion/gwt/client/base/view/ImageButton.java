package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.BaseImage;

public abstract class ImageButton extends FormButton {

    private boolean focusable = true;

    private boolean vertical;

    public ImageButton(String caption, BaseImage image, boolean vertical, Element element) {
        super(element);

        addStyleName("btn-image");
        addStyleName("btn-outline-secondary");

        this.vertical = vertical;

        BaseImage.initImageText(this, caption, image, vertical);

//        imgElement.addClassName("btn-image-img"); ???
    }

    protected abstract BaseImage getImage();
    protected abstract String getCaption();
    protected abstract boolean forceDiv();

    public void updateImage() {
        BaseImage.updateImage(getImage(), this, vertical);
    }

    public void updateText() {
        BaseImage.updateText(this, getCaption(), vertical, forceDiv());
    }

    // call before attach
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        if (!focusable) {
            setTabIndex(-1);
        }
    }
}
