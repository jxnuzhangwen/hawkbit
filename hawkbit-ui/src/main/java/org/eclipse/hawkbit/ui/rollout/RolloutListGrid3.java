package org.eclipse.hawkbit.ui.rollout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.eventbus.event.RolloutChangeEvent;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus.Status;
import org.eclipse.hawkbit.ui.distributionbar.renderers.HtmlButtonRenderer;
import org.eclipse.hawkbit.ui.distributionbar.renderers.LinkRenderer;
import org.eclipse.hawkbit.ui.rollout.RolloutListTable.ACTION;
import org.eclipse.hawkbit.ui.rollout.RolloutListTable.ContextMenuData;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.peter.contextmenu.ContextMenu;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuItem;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuItemClickEvent;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

@SpringComponent
@ViewScope
public class RolloutListGrid3 extends AbstractSimpleGrid {
    private static final long serialVersionUID = 4060904914954370524L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient RolloutManagement rolloutManagement;

    @Autowired
    private AddUpdateRolloutWindowLayout addUpdateRolloutWindow;

    @Autowired
    private UINotification uiNotification;

    @Autowired
    private transient RolloutUIState rolloutUIState;

    @Autowired
    private transient SpPermissionChecker permissionChecker;

    @Override
    @PostConstruct
    protected void init() {
        super.init();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final RolloutEvent event) {
        if (event == RolloutEvent.FILTER_BY_TEXT || event == RolloutEvent.CREATE_ROLLOUT
                || event == RolloutEvent.UPDATE_ROLLOUT || event == RolloutEvent.SHOW_ROLLOUTS) {
            refreshTable();
        }
    }

    /**
     * Handles the RolloutChangeEvent to refresh the item in the table.
     * 
     * @param rolloutChangeEvent
     *            the event which contains the rollout which has been changed
     */
    @SuppressWarnings("unchecked")
    @EventBusListenerMethod(scope = EventScope.SESSION)
    public void onEvent(final RolloutChangeEvent rolloutChangeEvent) {
        final Rollout rollout = rolloutManagement.findRolloutWithDetailedStatus(rolloutChangeEvent.getRolloutId());
        final TotalTargetCountStatus totalTargetCountStatus = rollout.getTotalTargetCountStatus();
        final LazyQueryContainer rolloutContainer = (LazyQueryContainer) getContainerDataSource();
        final Item item = rolloutContainer.getItem(rolloutChangeEvent.getRolloutId());
        item.getItemProperty(SPUILabelDefinitions.VAR_STATUS).setValue(rollout.getStatus());
        item.getItemProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setValue(totalTargetCountStatus);
        final Long groupCount = (Long) item.getItemProperty(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).getValue();
        if (null != rollout.getRolloutGroups() && groupCount != rollout.getRolloutGroups().size()) {
            item.getItemProperty(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS)
                    .setValue(Long.valueOf(rollout.getRolloutGroups().size()));
        }
    }

    @Override
    protected String getTableId() {
        return SPUIComponetIdProvider.ROLLOUT_LIST_TABLE_ID;
    }

    @Override
    protected Container createContainer() {
        final BeanQueryFactory<RolloutBeanQuery> rolloutQf = new BeanQueryFactory<>(RolloutBeanQuery.class);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), rolloutQf);
    }

    @Override
    protected void addContainerProperties() {
        final LazyQueryContainer rolloutGridContainer = (LazyQueryContainer) getContainerDataSource();
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_ID, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", false, false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_STATUS, RolloutStatus.class, null, false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_DIST_NAME_VERSION, String.class, null, false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null, false,
                false);

        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_DATE, String.class, null, false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_USER, String.class, null, false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_BY, String.class, null, false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS, Integer.class, 0, false,
                false);
        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS, String.class, "0", false,
                false);

        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS,
                TotalTargetCountStatus.class, null, false, false);

        rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.ACTION, String.class,
                FontAwesome.CIRCLE_O.getHtml(), false, false);

    }

    @Override
    protected void setColumnProperties() {
        List<Object> columnList = new ArrayList<>();
        columnList.add(SPUILabelDefinitions.VAR_NAME);
        columnList.add(SPUILabelDefinitions.VAR_DIST_NAME_VERSION);
        columnList.add(SPUILabelDefinitions.VAR_STATUS);
        columnList.add(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS);
        columnList.add(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS);
        columnList.add(SPUILabelDefinitions.VAR_TOTAL_TARGETS);
        columnList.add(SPUILabelDefinitions.VAR_CREATED_DATE);
        columnList.add(SPUILabelDefinitions.VAR_CREATED_USER);
        columnList.add(SPUILabelDefinitions.VAR_MODIFIED_DATE);
        columnList.add(SPUILabelDefinitions.VAR_MODIFIED_BY);
        columnList.add(SPUILabelDefinitions.VAR_DESC);
        columnList.add(SPUILabelDefinitions.ACTION);
        setColumnOrder(columnList.toArray());
        
        alignColumns();
    }

    private void alignColumns() {
        setCellStyleGenerator(new CellStyleGenerator() {
            private static final long serialVersionUID = 5573570647129792429L;

            @Override
            public String getStyle(final CellReference cellReference) {
                String[] coulmnNames = { SPUILabelDefinitions.VAR_STATUS,
                        SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS, SPUILabelDefinitions.ACTION };
                if (Arrays.asList(coulmnNames).contains(cellReference.getPropertyId())) {
                    return "centeralign";
                }
                return null;
            }
        });
    }

    @Override
    protected void setHiddenColumns() {
        List<Object> columnsToBeHidden = new ArrayList<>();
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_CREATED_DATE);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_CREATED_USER);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_MODIFIED_DATE);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_MODIFIED_BY);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_DESC);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_ID);
        for (Object propertyId : columnsToBeHidden) {
            getColumn(propertyId).setHidden(true);
        }
    }

    @Override
    protected void setColumnHeaderNames() {
        getColumn(SPUILabelDefinitions.VAR_NAME).setHeaderCaption(i18n.get("header.name"));
        getColumn(SPUILabelDefinitions.VAR_DIST_NAME_VERSION).setHeaderCaption(i18n.get("header.distributionset"));
        getColumn(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setHeaderCaption(i18n.get("header.numberofgroups"));
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setHeaderCaption(i18n.get("header.total.targets"));
        getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setHeaderCaption(i18n.get("header.createdDate"));
        getColumn(SPUILabelDefinitions.VAR_CREATED_USER).setHeaderCaption(i18n.get("header.createdBy"));
        getColumn(SPUILabelDefinitions.VAR_MODIFIED_DATE).setHeaderCaption(i18n.get("header.modifiedDate"));
        getColumn(SPUILabelDefinitions.VAR_MODIFIED_BY).setHeaderCaption(i18n.get("header.modifiedBy"));
        getColumn(SPUILabelDefinitions.VAR_DESC).setHeaderCaption(i18n.get("header.description"));
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS)
                .setHeaderCaption(i18n.get("header.detail.status"));
        getColumn(SPUILabelDefinitions.VAR_STATUS).setHeaderCaption(i18n.get("header.status"));
        getColumn(SPUILabelDefinitions.ACTION).setHeaderCaption(i18n.get("upload.action"));
    }

    @Override
    protected void setColumnExpandRatio() {
        getColumn(SPUILabelDefinitions.VAR_NAME).setExpandRatio(1);
        getColumn(SPUILabelDefinitions.VAR_NAME).setMaximumWidth(300);

        getColumn(SPUILabelDefinitions.VAR_DIST_NAME_VERSION).setExpandRatio(1);
        getColumn(SPUILabelDefinitions.VAR_DIST_NAME_VERSION).setMaximumWidth(300);

        getColumn(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setMinimumWidth(95);

        getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_CREATED_USER).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_MODIFIED_DATE).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_MODIFIED_BY).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_DESC).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_STATUS).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.VAR_STATUS).setMinimumWidth(75);

        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setExpandRatio(2);
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setMinimumWidth(600);

        getColumn(SPUILabelDefinitions.ACTION).setExpandRatio(0);
        getColumn(SPUILabelDefinitions.ACTION).setMinimumWidth(90);

    }

    @Override
    protected void addColumnRenderes() {
        setEditorEnabled(true);
        addDetailStatusColumn();
        addStatusCoulmn();
        getColumn(SPUILabelDefinitions.ACTION).setRenderer(new HtmlButtonRenderer(event -> onClickOfActionBtn(event)));
        getColumn(SPUILabelDefinitions.VAR_NAME).setRenderer(new LinkRenderer(event -> onClickOfRolloutName(event)));
    }

    private void onClickOfRolloutName(RendererClickEvent event) {
        rolloutUIState.setRolloutId((long) event.getItemId());
        final String rolloutName = (String) getContainerDataSource().getItem(event.getItemId())
                .getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        rolloutUIState.setRolloutName(rolloutName);
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUPS);
    }

    private void onClickOfActionBtn(RendererClickEvent event) {
        final ContextMenu contextMenu = createContextMenu((Long) event.getItemId());
        contextMenu.setAsContextMenuOf((AbstractClientConnector) event.getComponent());
        contextMenu.open(event.getClientX(), event.getClientY());
    }

    private ContextMenu createContextMenu(final Long rolloutId) {
        final Item row = getContainerDataSource().getItem(rolloutId);
        final RolloutStatus rolloutStatus = (RolloutStatus) row.getItemProperty(SPUILabelDefinitions.VAR_STATUS)
                .getValue();
        final ContextMenu context = new ContextMenu();
        context.addItemClickListener(event -> menuItemClicked(event));
        if (rolloutStatus == RolloutStatus.READY) {
            final ContextMenuItem startItem = context.addItem("Start");
            startItem.setData(new ContextMenuData(rolloutId, ACTION.START));
        } else if (rolloutStatus == RolloutStatus.RUNNING) {
            final ContextMenuItem pauseItem = context.addItem("Pause");
            pauseItem.setData(new ContextMenuData(rolloutId, ACTION.PAUSE));
        } else if (rolloutStatus == RolloutStatus.PAUSED) {
            final ContextMenuItem resumeItem = context.addItem("Resume");
            resumeItem.setData(new ContextMenuData(rolloutId, ACTION.RESUME));
        } else if (rolloutStatus == RolloutStatus.STARTING || rolloutStatus == RolloutStatus.CREATING) {
            return context;
        }
        if (permissionChecker.hasRolloutUpdatePermission()) {
            final ContextMenuItem cancelItem = context.addItem("Update");
            cancelItem.setData(new ContextMenuData(rolloutId, ACTION.UPDATE));
        }
        return context;
    }

    private void addDetailStatusColumn() {
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setRenderer(
                new org.eclipse.hawkbit.ui.distributionbar.renderers.StringDistributionBarRenderer(),
                new Converter<String, TotalTargetCountStatus>() {
                    private static final long serialVersionUID = 2660476405836705932L;

                    @Override
                    public TotalTargetCountStatus convertToModel(String value,
                            Class<? extends TotalTargetCountStatus> targetType, Locale locale)
                                    throws com.vaadin.data.util.converter.Converter.ConversionException {
                        return null;
                    }

                    @Override
                    public String convertToPresentation(TotalTargetCountStatus value,
                            Class<? extends String> targetType, Locale locale)
                                    throws com.vaadin.data.util.converter.Converter.ConversionException {
                        return getFormattedString(value.getStatusTotalCountMap());
                    }

                    @Override
                    public Class<TotalTargetCountStatus> getModelType() {
                        return TotalTargetCountStatus.class;
                    }

                    @Override
                    public Class<String> getPresentationType() {
                        return String.class;
                    }
                });
    }

    private void addStatusCoulmn() {
        getColumn(SPUILabelDefinitions.VAR_STATUS).setRenderer(new HtmlRenderer(),
                new Converter<String, RolloutStatus>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public RolloutStatus convertToModel(final String value,
                            final Class<? extends RolloutStatus> targetType, final Locale locale) {
                        return null;
                    }

                    @Override
                    public String convertToPresentation(final RolloutStatus value,
                            final Class<? extends String> targetType, final Locale locale) {
                        String result = null;
                        switch (value) {
                        case FINISHED:
                            result = "<div class=\"statusIconGreen\">" + FontAwesome.CHECK_CIRCLE.getHtml() + "</div>";
                            break;
                        case PAUSED:
                            result = "<div class=\"statusIconBlue\">" + FontAwesome.PAUSE.getHtml() + "</div>";
                            break;
                        case RUNNING:
                            result = "<div class=\"yellowSpinner\"></div>";
                            break;
                        case READY:
                            result = "<div class=\"statusIconLightBlue\"> <span title=\"xxx\">"
                                    + FontAwesome.DOT_CIRCLE_O.getHtml() + "</span></div>";
                            break;
                        case STOPPED:
                            result = "<div class=\"statusIconRed\">" + FontAwesome.STOP.getHtml() + "</div>";
                            break;
                        case CREATING:
                            result = "<div class=\"greySpinner\"></div>";
                            break;
                        case STARTING:
                            result = "<div class=\"blueSpinner\"></div>";
                            break;
                        case ERROR_CREATING:
                            result = "<div class=\"statusIconRed\">" + FontAwesome.EXCLAMATION_CIRCLE.getHtml()
                                    + "</div>";
                            break;
                        case ERROR_STARTING:
                            result = "<div class=\"statusIconRed\">" + FontAwesome.EXCLAMATION_CIRCLE.getHtml()
                                    + "</div>";
                            break;
                        default:
                            break;
                        }
                        return result;
                    }

                    @Override
                    public Class<RolloutStatus> getModelType() {
                        return RolloutStatus.class;
                    }

                    @Override
                    public Class<String> getPresentationType() {
                        return String.class;
                    }
                });
    }

    private String getFormattedString(Map<Status, Long> details) {
        StringBuilder val = new StringBuilder();
        String finalVal = null;
        if (null != details && !details.isEmpty()) {
            for (Entry<Status, Long> entry : details.entrySet()) {
                val.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
            }
            finalVal = val.substring(0, val.length() - 1);
        }
        return finalVal;
    }

    private void menuItemClicked(final ContextMenuItemClickEvent event) {
        final ContextMenuItem item = (ContextMenuItem) event.getSource();
        final ContextMenuData contextMenuData = (ContextMenuData) item.getData();
        final Item row = getContainerDataSource().getItem(contextMenuData.getRolloutId());
        final String rolloutName = (String) row.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        if (contextMenuData.getAction() == ACTION.PAUSE) {
            rolloutManagement.pauseRollout(rolloutManagement.findRolloutById(contextMenuData.getRolloutId()));
            uiNotification.displaySuccess(i18n.get("message.rollout.paused", rolloutName));
        } else if (contextMenuData.getAction() == ACTION.RESUME) {
            rolloutManagement.resumeRollout(rolloutManagement.findRolloutById(contextMenuData.getRolloutId()));
            uiNotification.displaySuccess(i18n.get("message.rollout.resumed", rolloutName));
        } else if (contextMenuData.getAction() == ACTION.START) {
            rolloutManagement.startRolloutAsync(rolloutManagement.findRolloutByName(rolloutName));
            uiNotification.displaySuccess(i18n.get("message.rollout.started", rolloutName));
        } else if (contextMenuData.getAction() == ACTION.UPDATE) {
            addUpdateRolloutWindow.populateData(contextMenuData.getRolloutId());
            final Window addTargetWindow = addUpdateRolloutWindow.getWindow();
            addTargetWindow.setCaption(i18n.get("caption.update.rollout"));
            UI.getCurrent().addWindow(addTargetWindow);
            addTargetWindow.setVisible(Boolean.TRUE);
        }
    }

    private void refreshTable() {
        ((LazyQueryContainer) getContainerDataSource()).refresh();
    }

    public final class FontIconGenerator extends PropertyValueGenerator<String> {

        private static final long serialVersionUID = 2544026030795375748L;
        private final FontAwesome fontIcon;

        public FontIconGenerator(FontAwesome icon) {
            this.fontIcon = icon;
        }

        @Override
        public String getValue(Item item, Object itemId, Object propertyId) {
            return fontIcon.getHtml();
        }

        @Override
        public Class<String> getType() {
            return String.class;
        }
    }

}
