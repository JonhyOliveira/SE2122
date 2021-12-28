package org.jabref.gui.groups;

import de.saxsys.mvvmfx.utils.validation.*;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;
import org.h2.util.StringUtils;
import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.groups.*;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SearchRules.SearchFlags;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.PreferencesService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GroupDialogViewModel {
    // Basic Settings
    private final StringProperty nameProperty = new SimpleStringProperty("");
    private final StringProperty descriptionProperty = new SimpleStringProperty("");
    private final StringProperty iconProperty = new SimpleStringProperty("");
    private final ObjectProperty<Color> colorProperty = new SimpleObjectProperty<>();
    private final ListProperty<GroupHierarchyType> groupHierarchyListProperty = new SimpleListProperty<>();
    private final ObjectProperty<GroupHierarchyType> groupHierarchySelectedProperty = new SimpleObjectProperty<>();

    // Type
    private final BooleanProperty typeExplicitProperty = new SimpleBooleanProperty();
    private final BooleanProperty typeKeywordsProperty = new SimpleBooleanProperty();
    private final BooleanProperty typeSearchProperty = new SimpleBooleanProperty();
    private final BooleanProperty typeAutoProperty = new SimpleBooleanProperty();
    private final BooleanProperty typeTexProperty = new SimpleBooleanProperty();
    private final BooleanProperty typeRefinedProperty = new SimpleBooleanProperty();

    // Option Groups
    private final StringProperty keywordGroupSearchTermProperty = new SimpleStringProperty("");
    private final StringProperty keywordGroupSearchFieldProperty = new SimpleStringProperty("");
    private final BooleanProperty keywordGroupCaseSensitiveProperty = new SimpleBooleanProperty();
    private final BooleanProperty keywordGroupRegexProperty = new SimpleBooleanProperty();

    private final StringProperty searchGroupSearchTermProperty = new SimpleStringProperty("");
    private final ObjectProperty<EnumSet<SearchFlags>> searchFlagsProperty = new SimpleObjectProperty(EnumSet.noneOf(SearchFlags.class));

    private final BooleanProperty autoGroupKeywordsOptionProperty = new SimpleBooleanProperty();
    private final StringProperty autoGroupKeywordsFieldProperty = new SimpleStringProperty("");
    private final StringProperty autoGroupKeywordsDelimiterProperty = new SimpleStringProperty("");
    private final StringProperty autoGroupKeywordsHierarchicalDelimiterProperty = new SimpleStringProperty("");
    private final BooleanProperty autoGroupPersonsOptionProperty = new SimpleBooleanProperty();
    private final StringProperty autoGroupPersonsFieldProperty = new SimpleStringProperty("");
    private final BooleanProperty refinedNumberProperty = new SimpleBooleanProperty();
    private final StringProperty refinedFieldNameProperty = new SimpleStringProperty();
    private final StringProperty numberFromRefinedProperty = new SimpleStringProperty();
    private final StringProperty numberToRefinedProperty = new SimpleStringProperty();
    private final IntegerProperty intFromRefinedProperty = new SimpleIntegerProperty();
    private final IntegerProperty intToRefinedProperty = new SimpleIntegerProperty();
    private final BooleanProperty refinedDateProperty = new SimpleBooleanProperty();
    private final ObjectProperty<LocalDate> dateFromRefinedProperty = new SimpleObjectProperty<LocalDate>();
    private final ObjectProperty<LocalDate> dateToRefinedProperty = new SimpleObjectProperty<LocalDate>();

    private final StringProperty texGroupFilePathProperty = new SimpleStringProperty("");

    private Validator nameValidator;
    private Validator nameContainsDelimiterValidator;
    private Validator sameNameValidator;
    private Validator keywordRegexValidator;
    private Validator keywordFieldEmptyValidator;
    private Validator keywordSearchTermEmptyValidator;
    private Validator searchRegexValidator;
    private Validator searchSearchTermEmptyValidator;
    private Validator texGroupFilePathValidator;

    private Validator refinedFieldNameValidator;
    private BooleanProperty refinedFieldNameIsValid = new SimpleBooleanProperty(false);
    private Validator refinedFromDateValidator, refinedToDateValidator, refinedOrderDateValidator;
    private BooleanProperty refinedOrderDateIsValid = new SimpleBooleanProperty(false);
    private Validator refinedFromNumberValidator, refinedToNumberValidator, refinedOrderNumberValidator;
    private final CompositeValidator validator = new CompositeValidator();

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final BibDatabaseContext currentDatabase;
    private final AbstractGroup editedGroup;
    private final GroupDialogHeader groupDialogHeader;

    public GroupDialogViewModel(DialogService dialogService, BibDatabaseContext currentDatabase, PreferencesService preferencesService, AbstractGroup editedGroup, GroupDialogHeader groupDialogHeader) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.currentDatabase = currentDatabase;
        this.editedGroup = editedGroup;
        this.groupDialogHeader = groupDialogHeader;

        setupValidation();
        setValues();
    }

    private void setupValidation() {
        nameValidator = new FunctionBasedValidator<>(
                nameProperty,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Please enter a name for the group.")));

        nameContainsDelimiterValidator = new FunctionBasedValidator<>(
                nameProperty,
                name -> !name.contains(Character.toString(preferencesService.getKeywordDelimiter())),
                ValidationMessage.warning(
                        Localization.lang(
                                "The group name contains the keyword separator \"%0\" and thus probably does not work as expected.",
                                Character.toString(preferencesService.getKeywordDelimiter())
                        )));

        sameNameValidator = new FunctionBasedValidator<>(
                nameProperty,
                name -> {
                    Optional<GroupTreeNode> rootGroup = currentDatabase.getMetaData().getGroups();
                    if (rootGroup.isPresent()) {
                        int groupsWithSameName = rootGroup.get().findChildrenSatisfying(group -> group.getName().equals(name)).size();
                        if ((editedGroup == null) && (groupsWithSameName > 0)) {
                            // New group but there is already one group with the same name
                            return false;
                        }

                        // Edit group, changed name to something that is already present
                        return (editedGroup == null) || editedGroup.getName().equals(name) || (groupsWithSameName <= 0);
                    }
                    return true;
                },
                ValidationMessage.warning(
                        Localization.lang("There exists already a group with the same name.") + "\n" +
                                Localization.lang("If you use it, it will inherit all entries from this other group.")
                )
        );

        keywordRegexValidator = new FunctionBasedValidator<>(
                keywordGroupSearchTermProperty,
                input -> {
                    if (!keywordGroupRegexProperty.getValue()) {
                        return true;
                    }

                    if (StringUtil.isNullOrEmpty(input)) {
                        return false;
                    }

                    try {
                        Pattern.compile(input);
                        return true;
                    } catch (PatternSyntaxException ignored) {
                        return false;
                    }
                },
                ValidationMessage.error(String.format("%s > %n %s %n %n %s",
                        Localization.lang("Searching for a keyword"),
                        Localization.lang("Keywords"),
                        Localization.lang("Invalid regular expression."))));

        keywordFieldEmptyValidator = new FunctionBasedValidator<>(
                keywordGroupSearchFieldProperty,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Please enter a field name to search for a keyword.")));

        keywordSearchTermEmptyValidator = new FunctionBasedValidator<>(
                keywordGroupSearchTermProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error(String.format("%s > %n %s %n %n %s",
                        Localization.lang("Searching for a keyword"),
                        Localization.lang("Keywords"),
                        Localization.lang("Search term is empty.")
                )));

        searchRegexValidator = new FunctionBasedValidator<>(
                searchGroupSearchTermProperty,
                input -> {
                    if (!searchFlagsProperty.getValue().contains(SearchRules.SearchFlags.CASE_SENSITIVE)) {
                        return true;
                    }

                    if (StringUtil.isNullOrEmpty(input)) {
                        return false;
                    }

                    try {
                        Pattern.compile(input);
                        return true;
                    } catch (PatternSyntaxException ignored) {
                        return false;
                    }
                },
                ValidationMessage.error(String.format("%s > %n %s",
                        Localization.lang("Free search expression"),
                        Localization.lang("Invalid regular expression."))));

        searchSearchTermEmptyValidator = new FunctionBasedValidator<>(
                searchGroupSearchTermProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error(String.format("%s > %n %s",
                        Localization.lang("Free search expression"),
                        Localization.lang("Search term is empty."))));

        ChangeListener<Object> refinedFieldCheck = (observable, oldValue, newValue) -> {
            String s = refinedFieldNameProperty.getValue();
            if (StringUtils.isNullOrEmpty(s)) {
                refinedFieldNameIsValid.setValue(false);
                return;
            }
            Field field = FieldFactory.parseField(s);
            refinedFieldNameIsValid.setValue((field.isNumeric() && refinedNumberProperty.getValue())
                    || (field.getProperties().contains(FieldProperty.DATE) && refinedDateProperty.getValue()));
        };

        refinedFieldNameValidator = new FunctionBasedValidator<>(
                refinedFieldNameIsValid,
                b -> b,
                ValidationMessage.error("Field must be a standard field.")
        );
        refinedNumberProperty.addListener(refinedFieldCheck);
        refinedDateProperty.addListener(refinedFieldCheck);
        refinedFieldNameProperty.addListener(refinedFieldCheck);

        refinedFromNumberValidator = new FunctionBasedValidator<>(
                numberFromRefinedProperty,
                s -> !StringUtils.isNullOrEmpty(s) && StringUtils.isNumber(s),
                ValidationMessage.error("Field must be a number")
        );

        refinedToNumberValidator = new FunctionBasedValidator<>(
                numberToRefinedProperty,
                s -> !StringUtils.isNullOrEmpty(s) && StringUtils.isNumber(s),
                ValidationMessage.error("Field must be a number")
        );

        refinedOrderNumberValidator = new CompositeValidator(
                new FunctionBasedValidator<>(
                        intToRefinedProperty.greaterThanOrEqualTo(intFromRefinedProperty),
                        input -> input,
                        ValidationMessage.error("To must be greater than from")
                )
        );

        ChangeListener<LocalDate> refinedOrderDateListener = (observable, oldValue, newValue) -> {
            LocalDate from  = dateFromRefinedProperty.getValue();
            LocalDate to = dateToRefinedProperty.getValue();
            if(from == null && to == null){
                refinedOrderDateIsValid.setValue(false);
            }
            else {
                refinedOrderDateIsValid.setValue(from == null || to == null ||
                        from.isBefore(to));
            }
        };

        refinedOrderDateValidator = new FunctionBasedValidator<>(
                refinedOrderDateIsValid,
                b -> b,
                ValidationMessage.error("To must be greater than from")
        );
        dateFromRefinedProperty.addListener(refinedOrderDateListener);
        dateToRefinedProperty.addListener(refinedOrderDateListener);

        texGroupFilePathValidator = new FunctionBasedValidator<>(
                texGroupFilePathProperty,
                input -> {
                    if (StringUtil.isBlank(input)) {
                        return false;
                    } else {
                        Path inputPath = getAbsoluteTexGroupPath(input);
                        if (!inputPath.isAbsolute() || !Files.isRegularFile(inputPath)) {
                            return false;
                        }
                        return FileUtil.getFileExtension(input)
                                .map(extension -> extension.toLowerCase().equals("aux"))
                                .orElse(false);
                    }
                },
                ValidationMessage.error(Localization.lang("Please provide a valid aux file.")));

        typeSearchProperty.addListener((obs, _oldValue, isSelected) -> {
            if (isSelected) {
                validator.addValidators(searchRegexValidator, searchSearchTermEmptyValidator);
            } else {
                validator.removeValidators(searchRegexValidator, searchSearchTermEmptyValidator);
            }
        });

        typeKeywordsProperty.addListener((obs, _oldValue, isSelected) -> {
            if (isSelected) {
                validator.addValidators(keywordFieldEmptyValidator, keywordRegexValidator, keywordSearchTermEmptyValidator);
            } else {
                validator.removeValidators(keywordFieldEmptyValidator, keywordRegexValidator, keywordSearchTermEmptyValidator);
            }
        });

        typeTexProperty.addListener((obs, oldValue, isSelected) -> {
            if (isSelected) {
                validator.addValidators(texGroupFilePathValidator);
            } else {
                validator.removeValidators(texGroupFilePathValidator);
            }
        });

        Validator numberValidators = new CompositeValidator(refinedFromNumberValidator, refinedToNumberValidator, refinedOrderNumberValidator);
        Validator dateValidators = new CompositeValidator(refinedOrderDateValidator); // TODO

        typeRefinedProperty.addListener((observable, oldValue, isSelected) -> {
            if (isSelected) {
                validator.addValidators(refinedFieldNameValidator);
                if (refinedNumberProperty.getValue())
                    validator.addValidators(numberValidators);
                if (refinedDateProperty.getValue())
                    validator.addValidators(dateValidators);

            } else {
                validator.removeValidators(numberValidators, dateValidators, refinedFieldNameValidator);
            }

        });

        refinedNumberProperty.addListener((obs, oldValue, isSelected) -> {
            if (isSelected && typeRefinedProperty.getValue()) {
                validator.addValidators(numberValidators);
            } else {
                validator.removeValidators(numberValidators);
            }
        });

        refinedDateProperty.addListener((observable, oldValue, isSelected) -> {
            if (isSelected && typeRefinedProperty.getValue()) {
                validator.addValidators(dateValidators);
            } else {
                validator.removeValidators(dateValidators);
            }
        });

    }

    /**
     * Gets the absolute path relative to the LatexFileDirectory, if given a relative path
     *
     * @param input the user input path
     * @return an absolute path if LatexFileDirectory exists; otherwise, returns input
     */
    private Path getAbsoluteTexGroupPath(String input) {
        Optional<Path> latexFileDirectory = currentDatabase.getMetaData().getLatexFileDirectory(preferencesService.getUser());
        return latexFileDirectory.map(path -> path.resolve(input)).orElse(Path.of(input));
    }

    public void validationHandler(Event event) {
        ValidationStatus validationStatus = validator.getValidationStatus();
        if (validationStatus.getHighestMessage().isPresent()) {
            dialogService.showErrorDialogAndWait(validationStatus.getHighestMessage().get().getMessage());
            // consume the event to prevent the dialog to close
            event.consume();
        }
    }

    public AbstractGroup resultConverter(ButtonType button) {
        if (button != ButtonType.OK) {
            return null;
        }

        AbstractGroup resultingGroup = null;
        try {
            String groupName = nameProperty.getValue().trim();
            if (typeExplicitProperty.getValue()) {
                resultingGroup = new ExplicitGroup(
                        groupName,
                        groupHierarchySelectedProperty.getValue(),
                        preferencesService.getKeywordDelimiter());
            } else if (typeKeywordsProperty.getValue()) {
                if (keywordGroupRegexProperty.getValue()) {
                    resultingGroup = new RegexKeywordGroup(
                            groupName,
                            groupHierarchySelectedProperty.getValue(),
                            FieldFactory.parseField(keywordGroupSearchFieldProperty.getValue().trim()),
                            keywordGroupSearchTermProperty.getValue().trim(),
                            keywordGroupCaseSensitiveProperty.getValue());
                } else {
                    resultingGroup = new WordKeywordGroup(
                            groupName,
                            groupHierarchySelectedProperty.getValue(),
                            FieldFactory.parseField(keywordGroupSearchFieldProperty.getValue().trim()),
                            keywordGroupSearchTermProperty.getValue().trim(),
                            keywordGroupCaseSensitiveProperty.getValue(),
                            preferencesService.getKeywordDelimiter(),
                            false);
                }
            } else if (typeSearchProperty.getValue()) {
                resultingGroup = new SearchGroup(
                        groupName,
                        groupHierarchySelectedProperty.getValue(),
                        searchGroupSearchTermProperty.getValue().trim(),
                        searchFlagsProperty.getValue());
            } else if (typeAutoProperty.getValue()) {
                if (autoGroupKeywordsOptionProperty.getValue()) {
                    // Set default value for delimiters: ',' for base and '>' for hierarchical
                    char delimiter = ',';
                    char hierarDelimiter = Keyword.DEFAULT_HIERARCHICAL_DELIMITER;
                    // Modify values for delimiters if user provided customized values
                    if (!autoGroupKeywordsDelimiterProperty.getValue().isEmpty()) {
                        delimiter = autoGroupKeywordsDelimiterProperty.getValue().charAt(0);
                    }
                    if (!autoGroupKeywordsHierarchicalDelimiterProperty.getValue().isEmpty()) {
                        hierarDelimiter = autoGroupKeywordsHierarchicalDelimiterProperty.getValue().charAt(0);
                    }
                    resultingGroup = new AutomaticKeywordGroup(
                            groupName,
                            groupHierarchySelectedProperty.getValue(),
                            FieldFactory.parseField(autoGroupKeywordsFieldProperty.getValue().trim()),
                            delimiter,
                            hierarDelimiter);
                } else {
                    resultingGroup = new AutomaticPersonsGroup(
                            groupName,
                            groupHierarchySelectedProperty.getValue(),
                            FieldFactory.parseField(autoGroupPersonsFieldProperty.getValue().trim()));
                }
            } else if (typeTexProperty.getValue()) {
                resultingGroup = TexGroup.create(
                        groupName,
                        groupHierarchySelectedProperty.getValue(),
                        Path.of(texGroupFilePathProperty.getValue().trim()),
                        new DefaultAuxParser(new BibDatabase()),
                        Globals.getFileUpdateMonitor(),
                        currentDatabase.getMetaData());
            } else if (typeRefinedProperty.getValue()) {
                Object from = refinedNumberProperty.getValue() ?
                        Integer.getInteger(numberFromRefinedProperty.getValue()) :
                        dateFromRefinedProperty.getValue();
                Object to = refinedNumberProperty.getValue() ?
                        Integer.getInteger(numberToRefinedProperty.getValue()) :
                        dateToRefinedProperty.getValue();
                resultingGroup = new RefinedGroup(
                        groupName,
                        groupHierarchySelectedProperty.getValue(),
                        from,
                        to,
                        FieldFactory.parseField(refinedFieldNameProperty.getValue().trim())
                );
            }

            if (resultingGroup != null) {
                resultingGroup.setColor(colorProperty.getValue());
                resultingGroup.setDescription(descriptionProperty.getValue());
                resultingGroup.setIconName(iconProperty.getValue());
                return resultingGroup;
            }

            return null;
        } catch (IllegalArgumentException | IOException exception) {
            dialogService.showErrorDialogAndWait(exception.getLocalizedMessage(), exception);
            return null;
        }
    }

    public void setValues() {
        groupHierarchyListProperty.setValue(FXCollections.observableArrayList(GroupHierarchyType.values()));

        if (editedGroup == null) {
            // creating new group -> defaults!
            colorProperty.setValue(IconTheme.getDefaultGroupColor());
            typeExplicitProperty.setValue(true);
            refinedDateProperty.setValue(true);
            groupHierarchySelectedProperty.setValue(GroupHierarchyType.INDEPENDENT);
        } else {
            nameProperty.setValue(editedGroup.getName());
            colorProperty.setValue(editedGroup.getColor().orElse(IconTheme.getDefaultGroupColor()));
            descriptionProperty.setValue(editedGroup.getDescription().orElse(""));
            iconProperty.setValue(editedGroup.getIconName().orElse(""));
            groupHierarchySelectedProperty.setValue(editedGroup.getHierarchicalContext());

            if (editedGroup.getClass() == WordKeywordGroup.class) {
                typeKeywordsProperty.setValue(true);

                WordKeywordGroup group = (WordKeywordGroup) editedGroup;
                keywordGroupSearchFieldProperty.setValue(group.getSearchField().getName());
                keywordGroupSearchTermProperty.setValue(group.getSearchExpression());
                keywordGroupCaseSensitiveProperty.setValue(group.isCaseSensitive());
                keywordGroupRegexProperty.setValue(false);
            } else if (editedGroup.getClass() == RegexKeywordGroup.class) {
                typeKeywordsProperty.setValue(true);

                RegexKeywordGroup group = (RegexKeywordGroup) editedGroup;
                keywordGroupSearchFieldProperty.setValue(group.getSearchField().getName());
                keywordGroupSearchTermProperty.setValue(group.getSearchExpression());
                keywordGroupCaseSensitiveProperty.setValue(group.isCaseSensitive());
                keywordGroupRegexProperty.setValue(true);
            } else if (editedGroup.getClass() == SearchGroup.class) {
                typeSearchProperty.setValue(true);

                SearchGroup group = (SearchGroup) editedGroup;
                searchGroupSearchTermProperty.setValue(group.getSearchExpression());
                searchFlagsProperty.setValue(group.getSearchFlags());
            } else if (editedGroup.getClass() == ExplicitGroup.class) {
                typeExplicitProperty.setValue(true);
            } else if (editedGroup instanceof AutomaticGroup) {
                typeAutoProperty.setValue(true);

                if (editedGroup.getClass() == AutomaticKeywordGroup.class) {
                    AutomaticKeywordGroup group = (AutomaticKeywordGroup) editedGroup;
                    autoGroupKeywordsDelimiterProperty.setValue(group.getKeywordDelimiter().toString());
                    autoGroupKeywordsHierarchicalDelimiterProperty.setValue(group.getKeywordHierarchicalDelimiter().toString());
                    autoGroupKeywordsFieldProperty.setValue(group.getField().getName());
                } else if (editedGroup.getClass() == AutomaticPersonsGroup.class) {
                    AutomaticPersonsGroup group = (AutomaticPersonsGroup) editedGroup;
                    autoGroupPersonsFieldProperty.setValue(group.getField().getName());
                }
            } else if (editedGroup.getClass() == TexGroup.class) {
                typeTexProperty.setValue(true);

                TexGroup group = (TexGroup) editedGroup;
                texGroupFilePathProperty.setValue(group.getFilePath().toString());
            } else if (editedGroup.getClass() == RefinedGroup.class) {
                typeRefinedProperty.setValue(true);

                RefinedGroup group = (RefinedGroup) editedGroup;
                refinedFieldNameProperty.setValue(group.getSearchField().getName());
                if (group.isNumberFilter()) {
                    refinedNumberProperty.setValue(true);
                    numberFromRefinedProperty.setValue(Objects.requireNonNullElse(group.getFromNumber(), "").toString());
                    numberToRefinedProperty.setValue(Objects.requireNonNullElse(group.getToNumber(), "").toString());
                } else {
                    refinedDateProperty.setValue(true);
                    dateFromRefinedProperty.setValue(group.getFromDate());
                    dateToRefinedProperty.setValue(group.getToDate());
                }
            }
        }
    }

    public void texGroupBrowse() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.AUX)
                .withDefaultExtension(StandardFileType.AUX)
                .withInitialDirectory(currentDatabase.getMetaData()
                        .getLatexFileDirectory(preferencesService.getUser())
                        .orElse(FileUtil.getInitialDirectory(currentDatabase, preferencesService))).build();
        dialogService.showFileOpenDialog(fileDialogConfiguration)
                .ifPresent(file -> texGroupFilePathProperty.setValue(
                        FileUtil.relativize(file.toAbsolutePath(), getFileDirectoriesAsPaths()).toString()
                ));
    }

    public void openHelpPage() {
        HelpAction.openHelpPage(HelpFile.GROUPS);
    }

    private List<Path> getFileDirectoriesAsPaths() {
        List<Path> fileDirs = new ArrayList<>();
        MetaData metaData = currentDatabase.getMetaData();
        metaData.getLatexFileDirectory(preferencesService.getFilePreferences().getUser()).ifPresent(fileDirs::add);

        return fileDirs;
    }

    public ValidationStatus validationStatus() {
        return validator.getValidationStatus();
    }

    public ValidationStatus nameValidationStatus() {
        return nameValidator.getValidationStatus();
    }

    public ValidationStatus nameContainsDelimiterValidationStatus() {
        return nameContainsDelimiterValidator.getValidationStatus();
    }

    public ValidationStatus sameNameValidationStatus() {
        return sameNameValidator.getValidationStatus();
    }

    public ValidationStatus searchRegexValidationStatus() {
        return searchRegexValidator.getValidationStatus();
    }

    public ValidationStatus searchSearchTermEmptyValidationStatus() {
        return searchSearchTermEmptyValidator.getValidationStatus();
    }

    public ValidationStatus keywordRegexValidationStatus() {
        return keywordRegexValidator.getValidationStatus();
    }

    public ValidationStatus keywordFieldEmptyValidationStatus() {
        return keywordFieldEmptyValidator.getValidationStatus();
    }

    public ValidationStatus keywordSearchTermEmptyValidationStatus() {
        return keywordSearchTermEmptyValidator.getValidationStatus();
    }

    public ValidationStatus texGroupFilePathValidatonStatus() {
        return texGroupFilePathValidator.getValidationStatus();
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public StringProperty descriptionProperty() {
        return descriptionProperty;
    }

    public StringProperty iconProperty() {
        return iconProperty;
    }

    public ObjectProperty<Color> colorFieldProperty() {
        return colorProperty;
    }

    public ListProperty<GroupHierarchyType> groupHierarchyListProperty() {
        return groupHierarchyListProperty;
    }

    public ObjectProperty<GroupHierarchyType> groupHierarchySelectedProperty() {
        return groupHierarchySelectedProperty;
    }

    public BooleanProperty typeExplicitProperty() {
        return typeExplicitProperty;
    }

    public BooleanProperty typeKeywordsProperty() {
        return typeKeywordsProperty;
    }

    public BooleanProperty typeSearchProperty() {
        return typeSearchProperty;
    }

    public BooleanProperty typeAutoProperty() {
        return typeAutoProperty;
    }

    public BooleanProperty typeTexProperty() {
        return typeTexProperty;
    }

    public BooleanProperty typeRefinedProperty() {
        return typeRefinedProperty;
    }

    public StringProperty keywordGroupSearchTermProperty() {
        return keywordGroupSearchTermProperty;
    }

    public StringProperty keywordGroupSearchFieldProperty() {
        return keywordGroupSearchFieldProperty;
    }

    public BooleanProperty keywordGroupCaseSensitiveProperty() {
        return keywordGroupCaseSensitiveProperty;
    }

    public BooleanProperty keywordGroupRegexProperty() {
        return keywordGroupRegexProperty;
    }

    public StringProperty searchGroupSearchTermProperty() {
        return searchGroupSearchTermProperty;
    }

    public ObjectProperty<EnumSet<SearchFlags>> searchFlagsProperty() {
        return searchFlagsProperty;
    }

    public BooleanProperty autoGroupKeywordsOptionProperty() {
        return autoGroupKeywordsOptionProperty;
    }

    public StringProperty autoGroupKeywordsFieldProperty() {
        return autoGroupKeywordsFieldProperty;
    }

    public StringProperty autoGroupKeywordsDeliminatorProperty() {
        return autoGroupKeywordsDelimiterProperty;
    }

    public StringProperty autoGroupKeywordsHierarchicalDeliminatorProperty() {
        return autoGroupKeywordsHierarchicalDelimiterProperty;
    }

    public BooleanProperty autoGroupPersonsOptionProperty() {
        return autoGroupPersonsOptionProperty;
    }

    public StringProperty autoGroupPersonsFieldProperty() {
        return autoGroupPersonsFieldProperty;
    }

    public StringProperty refinedFieldNameProperty() {
        return refinedFieldNameProperty;
    }

    public BooleanProperty refinedNumberProperty() {
        return refinedNumberProperty;
    }

    public BooleanProperty refinedDateProperty() {
        return refinedDateProperty;
    }

    public StringProperty numberFromRefinedProperty() {
        return numberFromRefinedProperty;
    }

    public StringProperty numberToRefinedProperty() {
        return numberToRefinedProperty;
    }

    public IntegerProperty intFromRefinedProperty() {
        return intFromRefinedProperty;
    }

    public IntegerProperty intToRefinedProperty() {
        return intToRefinedProperty;
    }

    public ObjectProperty<LocalDate> dateFromRefinedProperty() {
        return dateFromRefinedProperty;
    }

    public ObjectProperty<LocalDate> dateToRefinedProperty() {
        return dateToRefinedProperty;
    }

    public StringProperty texGroupFilePathProperty() {
        return texGroupFilePathProperty;
    }
}
