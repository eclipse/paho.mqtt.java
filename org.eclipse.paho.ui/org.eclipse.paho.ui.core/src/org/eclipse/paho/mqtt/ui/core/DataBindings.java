/*******************************************************************************
 * Copyright (c) 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Bin Zhang - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.paho.mqtt.ui.core;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.paho.mqtt.ui.Constants;
import org.eclipse.paho.mqtt.ui.core.model.Topic;
import org.eclipse.paho.mqtt.ui.nls.Messages;
import org.eclipse.paho.mqtt.ui.support.fieldassist.ControlDecorationSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;

/**
 * JFace DataBinding helper
 * 
 * @author Bin Zhang
 */
public final class DataBindings {

	/**
	 * @return DataBinding
	 */
	public static DataBinding createDataBinding() {
		return new DataBinding();
	}

	/**
	 * Wrapper of Binding and ControlDecoration support
	 */
	public static final class DecoratedBinding {
		private final Binding binding;
		private final ControlDecorationSupport decorationSupport;

		/**
		 * @param binding
		 * @param decorationSupport
		 */
		public DecoratedBinding(Binding binding, ControlDecorationSupport decorationSupport) {
			this.binding = binding;
			this.decorationSupport = decorationSupport;
		}

		public Binding get() {
			return binding;
		}

		public DecoratedBinding showDecorations() {
			// show decorations by validating the model
			binding.validateModelToTarget();
			binding.validateTargetToModel();
			return this;
		}

		public DecoratedBinding hideDecorations() {
			decorationSupport.hideDecorations();
			return this;
		}

		public void dispose() {
			binding.dispose();
			decorationSupport.dispose();
		}
	}

	// **************************************************************************
	// Converters
	// **************************************************************************
	public static final class Converters {

		public static final IConverter trimmedString = new Converter(Object.class, String.class) {
			@Override
			public Object convert(Object fromObject) {
				if (fromObject == null) {
					return null;
				}
				return ((String) fromObject).trim();
			}
		};

		public static final IConverter stringToBytes = new Converter(Object.class, byte[].class) {
			@Override
			public Object convert(Object fromObject) {
				if (fromObject == null) {
					return null;
				}
				
				return ((String) fromObject).trim().getBytes();
			}
		};

		public static final IConverter bytesToString = new Converter(byte[].class, String.class) {
			@Override
			public Object convert(Object fromObject) {
				if (fromObject == null) {
					return null;
				}

				return new String((byte[]) fromObject);
			}
		};

		public static final IConverter stringToChars = new Converter(Object.class, char[].class) {
			@Override
			public Object convert(Object fromObject) {
				if (fromObject == null) {
					return null;
				}
				System.err.println(fromObject);
				return ((String) fromObject).trim().toCharArray();
			}
		};
	}

	// **************************************************************************
	// Validators
	// **************************************************************************
	public static final class Validators {

		public static final IValidator required = new IValidator() {
			@Override
			public IStatus validate(Object value) {
				String str = value == null ? null : String.valueOf(value);
				if (str == null || str.trim().length() == 0) {
					return ValidationStatus.error(Messages.VALIDATION_VALUE_REQUIRED);
				}
				return ValidationStatus.ok();
			}
		};

		public static final IValidator serverUri = new IValidator() {
			@Override
			public IStatus validate(Object value) {
				String serverURI = (String) value;
				if (serverURI == null || serverURI.length() == 0) {
					return ValidationStatus.error(Messages.VALIDATION_SERVER_URI_REQUIRED);
				}

				try {
					URI srvURI = new URI(serverURI);
					if (!"".equals(srvURI.getPath())) {
						return ValidationStatus.error(Messages.VALIDATION_INVALID_SERVER_URI);
					}

					return Constants.MQTT_SCHEMES.contains(srvURI.getScheme()) ? ValidationStatus.ok()
							: ValidationStatus.error(Messages.VALIDATION_INVALID_SERVER_URI);

				}
				catch (URISyntaxException e) {
					return ValidationStatus.error(Messages.bind(Messages.VALIDATION_INVALID_SERVER_URI_MSG,
							e.getLocalizedMessage()));
				}
			}
		};

		public static final IValidator clientId = new IValidator() {
			@Override
			public IStatus validate(Object value) {
				String clientId = (String) value;
				if (clientId == null) {
					return ValidationStatus.error(Messages.VALIDATION_VALUE_REQUIRED);
				}

				// Count characters, surrogate pairs count as one character.
				int clientIdLength = 0;
				for (int i = 0; i < clientId.length() - 1; i++) {
					if (Character.isHighSurrogate(clientId.charAt(i))) {
						i++;
					}
					clientIdLength++;
				}
				if (clientIdLength > 65535) {
					return ValidationStatus.error(Messages.VALIDATION_INVALID_CLIENT_ID);
				}

				return ValidationStatus.ok();
			}
		};

		public static final IValidator publishTopic = new IValidator() {
			@Override
			public IStatus validate(Object value) {
				String topic = String.valueOf(value).trim();
				// topic string is required, cannot be null or empty
				if (topic == null || topic.length() == 0) {
					return ValidationStatus.error(Messages.VALIDATION_VALUE_REQUIRED);
				}

				try {
					// cannot have any wildcard chars
					Topic.validate(topic, false);
				}
				catch (IllegalArgumentException e) {
					return ValidationStatus.error(e.getLocalizedMessage());
				}

				return ValidationStatus.ok();
			}
		};

		public static final IValidator subscribeTopic = new IValidator() {
			@Override
			public IStatus validate(Object value) {
				String topic = String.valueOf(value).trim();
				// topic string is required, cannot be null or empty
				if (topic == null || topic.length() == 0) {
					return ValidationStatus.error(Messages.VALIDATION_VALUE_REQUIRED);
				}

				try {
					Topic.validate(topic, true);
				}
				catch (IllegalArgumentException e) {
					return ValidationStatus.error(e.getLocalizedMessage());
				}

				return ValidationStatus.ok();
			}
		};

		/**
		 * @param validator
		 * @param control
		 */
		public static IValidator decorate(IValidator validator, Control control) {
			return new DecoratedValidator(validator, control);
		}

		/**
		 * @param validator
		 * @param decoration
		 */
		public static IValidator decorate(IValidator validator, ControlDecoration decoration) {
			return new DecoratedValidator(validator, decoration);
		}

		/**
		 * Decorated Validator
		 */
		public static class DecoratedValidator implements IValidator {
			private final IValidator validator;
			private final ControlDecoration decoration;

			/**
			 * @param validator
			 * @param control
			 */
			protected DecoratedValidator(IValidator validator, Control control) {
				this(validator, new ControlDecoration(control, SWT.LEFT)); // SWT.LEFT| SWT.TOP
			}

			/**
			 * @param validator
			 * @param decoration
			 */
			protected DecoratedValidator(IValidator validator, ControlDecoration decoration) {
				this.validator = validator;
				this.decoration = decoration;
				this.decoration.setMarginWidth(10);
			}

			@Override
			public IStatus validate(Object value) {
				IStatus status = validator.validate(value);

				if (status == null || status.isOK()) {
					decoration.hide();
				}
				else {
					decoration.setImage(getImage(status));
					decoration.setDescriptionText(getDescriptionText(status));
					decoration.showHoverText(getDescriptionText(status));
					decoration.show();
				}
				return status;
			}

			/**
			 * Returns the description text to show in a ControlDecoration for the given status. The default
			 * implementation of this method returns status.getMessage().
			 * 
			 * @param status the status object.
			 * @return the description text to show in a ControlDecoration for the given status.
			 */
			protected String getDescriptionText(IStatus status) {
				return status == null ? "" : status.getMessage(); //$NON-NLS-1$
			}

			/**
			 * Returns an image to display in a ControlDecoration which is appropriate for the given status. The default
			 * implementation of this method returns an image according to <code>status.getSeverity()</code>:
			 * <ul>
			 * <li>IStatus.OK => No image
			 * <li>IStatus.INFO => FieldDecorationRegistry.DEC_INFORMATION
			 * <li>IStatus.WARNING => FieldDecorationRegistry.DEC_WARNING
			 * <li>IStatus.ERROR => FieldDecorationRegistry.DEC_ERROR
			 * <li>IStatus.CANCEL => FieldDecorationRegistry.DEC_ERROR
			 * <li>Other => No image
			 * </ul>
			 * 
			 * @param status the status object.
			 * @return an image to display in a ControlDecoration which is appropriate for the given status.
			 */
			protected Image getImage(IStatus status) {
				if (status == null)
					return null;

				String fieldDecorationID = null;
				switch (status.getSeverity()) {
				case IStatus.INFO:
					fieldDecorationID = FieldDecorationRegistry.DEC_INFORMATION;
					break;
				case IStatus.WARNING:
					fieldDecorationID = FieldDecorationRegistry.DEC_WARNING;
					break;
				case IStatus.ERROR:
				case IStatus.CANCEL:
					fieldDecorationID = FieldDecorationRegistry.DEC_ERROR;
					break;
				}

				FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
						fieldDecorationID);
				return fieldDecoration == null ? null : fieldDecoration.getImage();
			}
		}
	}

	/**
	 * DataBinding
	 */
	public static class DataBinding implements DisposeListener {
		// context
		private DataBindingContext context;

		public DataBinding() {
			this.context = new DataBindingContext();
		}

		/**
		 * @param control
		 * @param bean
		 * @param propertyName
		 */
		public Binding bindSelection(Control control, Object bean, String propertyName) {
			IObservableValue widgetValue = SWTObservables.observeSelection(control);
			IObservableValue modelValue = BeansObservables.observeValue(bean, propertyName);
			return context.bindValue(widgetValue, modelValue, null, null);
		}

		/**
		 * @param control
		 * @param bean
		 * @param propertyName
		 * @param validator
		 */
		public DecoratedBinding bindText(Control control, Object bean, String propertyName, IValidator validator) {
			return bindText(control, SWT.Modify, bean, propertyName, validator, Converters.trimmedString);
		}

		/**
		 * @param control
		 * @param bean
		 * @param propertyName
		 * @param converter
		 */
		public DecoratedBinding bindText(Control control, Object bean, String propertyName, IConverter converter) {
			return bindText(control, SWT.Modify, bean, propertyName, null, converter);
		}

		/**
		 * @param control
		 * @param bean
		 * @param propertyName
		 * @param validator
		 * @param converter
		 */
		public DecoratedBinding bindText(Control control, Object bean, String propertyName, IValidator validator,
				IConverter converter) {
			return bindText(control, SWT.Modify, bean, propertyName, validator, converter);
		}

		/**
		 * @param control
		 * @param bean
		 * @param propertyName
		 */
		public DecoratedBinding bindTextAsBytes(Control control, Object bean, String propertyName) {
			return bindTextAsBytes(control, bean, propertyName, null);
		}

		/**
		 * @param control
		 * @param bean
		 * @param propertyName
		 * @param validator
		 */
		public DecoratedBinding bindTextAsBytes(Control control, Object bean, String propertyName, IValidator validator) {
			return bindText(control, SWT.Modify, bean, propertyName, validator, Converters.stringToBytes,
					Converters.bytesToString);
		}

		/**
		 * @param control
		 * @param event
		 * @param bean
		 * @param propertyName
		 * @param validator
		 * @param targetToModelConverter
		 */
		public DecoratedBinding bindText(Control control, int event, Object bean, String propertyName,
				IValidator validator, IConverter targetToModelConverter) {
			return bindText(control, event, bean, propertyName, validator, targetToModelConverter, null);
		}

		/**
		 * Returns an observable observing the text attribute of the provided <code>control</code>. The supported types
		 * are:
		 * <ul>
		 * <li>org.eclipse.swt.widgets.Text</li>
		 * <li>org.eclipse.swt.custom.StyledText (as of 1.3)</li>
		 * </ul>
		 * 
		 * @param control
		 * @param event event type to register for change events {@literal SWT.FocusOut  SWT.Modify or SWT.NONE}
		 * @param bean
		 * @param propertyName
		 * @param validator
		 * @param converter
		 * @return binding
		 * @throws IllegalArgumentException if <code>control</code> type is unsupported
		 */
		public DecoratedBinding bindText(Control control, int event, Object bean, String propertyName,
				IValidator validator, IConverter targetToModelConverter, IConverter modelToTargetConverter) {

			// IObservableValue widgetValue =
			// WidgetProperties.text(SWT.Modify).observe(serverUriText);
			// IObservableValue modelValuea = BeanProperties.value(Connection.class,
			// "serverURI").observe(connection);

			// observe
			IObservableValue widgetValue = SWTObservables.observeText(control, event);
			IObservableValue modelValue = BeansObservables.observeValue(bean, propertyName);

			UpdateValueStrategy targetToModel = new UpdateValueStrategy();
			UpdateValueStrategy modelToTarget = new UpdateValueStrategy();

			// targetToModel
			// validator
			if (validator != null) {
				targetToModel.setBeforeSetValidator(validator);
			}
			// converter
			if (targetToModelConverter != null) {
				targetToModel.setConverter(targetToModelConverter);
			}

			// modelToTarget
			if (modelToTargetConverter != null) {
				modelToTarget.setConverter(modelToTargetConverter);
			}

			Binding bindValue = context.bindValue(widgetValue, modelValue, targetToModel, modelToTarget);

			// decoration
			ControlDecorationSupport decorationSupport = ControlDecorationSupport.create(bindValue, SWT.TOP | SWT.LEFT);

			return new DecoratedBinding(bindValue, decorationSupport);
		}

		/**
		 * @param changeListener
		 */
		public void onMergedValueChange(IValueChangeListener changeListener) {
			AggregateValidationStatus aggregateValidationStatus = new AggregateValidationStatus(context,
					AggregateValidationStatus.MERGED);
			aggregateValidationStatus.addValueChangeListener(changeListener);
		}

		@Override
		public void widgetDisposed(DisposeEvent e) {
			context.dispose();
		}

	}

}
