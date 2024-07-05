package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.InterfaceImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

/**
 * Test Service for the SecurityService class.
 */
@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

	private SecurityService securityService;
	// all sensors
	private HashSet<Sensor> listSensor;
	private Sensor sensor;

	@Mock
	private InterfaceImageService imageService;

	@Mock
	private SecurityRepository securityRepository;

	@Mock
	private StatusListener statusListener;

	/**
	 * Sets up the test environment before each test.
	 */
	@BeforeEach
	void init() {
		// Initialize the security service and a sensor before each test
		securityService = new SecurityService(securityRepository, imageService);
		// mock a sensor to all sensors
		listSensor = new HashSet<>();
		sensor = new Sensor("Sensor-01", SensorType.DOOR);
		sensor.setActive(true);
		listSensor.add(sensor);
		listSensor.add(new Sensor("Sensor-02", SensorType.MOTION));
		listSensor.add(new Sensor("Sensor-03", SensorType.WINDOW));
	}

	@ParameterizedTest(name = "Test case  using arming status: {0}")
	@EnumSource(value = ArmingStatus.class, names = { "ARMED_AWAY", "ARMED_HOME" })
	@DisplayName("1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.")
	void alarmIsArmed_sensorActivated_returnPendingAlarmStatus(ArmingStatus armingStatus) {
		when(securityService.getArmingStatus()).thenReturn(armingStatus);
		when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
		securityService.changeSensorActivationStatus(sensor, true);
		verify(securityRepository).updateSensor(any(Sensor.class));
		verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
	}

	@Test
	@DisplayName("2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.")
	public void alarmIsPending_sensorActivated_returnAlarmStatusToAlarm() {
		when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
		when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

		securityService.changeSensorActivationStatus(sensor, true);

		verify(securityRepository).updateSensor(any(Sensor.class));
		verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
	}

	@Test
	@DisplayName("3. If pending alarm and all sensors are inactive, return to no alarm state.")
	public void alarmIsPending_allSensorsInactive_returnNoAlarmState() {
		when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

		for (Sensor sensor : listSensor) {
			sensor.setActive(true);
			securityService.changeSensorActivationStatus(sensor, false);
		}

		verify(securityRepository, times(listSensor.size())).updateSensor(any(Sensor.class));
		verify(securityRepository, times(listSensor.size())).setAlarmStatus(AlarmStatus.NO_ALARM);
	}

	@Test
	@DisplayName("4. If alarm is active, change in sensor state should not affect the alarm state.")
	public void alarmIsActive_verifySensorStateNotAffectAlarmState() {
		when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

		securityService.changeSensorActivationStatus(sensor, false);

		verify(securityRepository).updateSensor(any(Sensor.class));
		// verify that sensor state should not affect the alarm state.
		verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
	}

	@Test
	@DisplayName("5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.")
	public void alarmPendingState_sensorActivated_whileAlreadyActive_returnAlarmChangeToAlarmState() {
		// The system is in pending state
		when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

		// while already active
		sensor.setActive(true);
		// a sensor is activated
		securityService.changeSensorActivationStatus(sensor, true);

		verify(securityRepository).updateSensor(any(Sensor.class));
		verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
	}

	@Test
	@DisplayName("6. If a sensor is deactivated while already inactive, make no changes to the alarm state.")
	public void sensorIsDeactivated_alreadyInactive_verifyNoChangeAlarmState() {
		// Sensor already inactive,
		Sensor deactivatedSensor = new Sensor("Sensor-02", SensorType.MOTION);
		deactivatedSensor.setActive(false);

		// Deactivated sensor
		securityService.changeSensorActivationStatus(deactivatedSensor, false);

		verify(securityRepository).updateSensor(any(Sensor.class));
		// verifyNoChangeAlarmState
		verify(securityRepository, never()).setAlarmStatus(any());
	}

	@Test
	@DisplayName("7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.")
	public void imageServiceIdentifyImageContainsCat_systemIsArmedHome_verifyChangeToAlarmStatus() {
		// Image service identifies an image containing a cat
		when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
		// while the system is armed-home,
		when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
	
		securityService.processImage(mock(BufferedImage.class));

		verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
	}

	@Test
	@DisplayName("8. If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active.")
	public void imageService_identifyImage_notContainsCat_sensorAreNotActive_verifyChangeToNoAlarmStatus() {
		// as long as the sensors are not active.
		listSensor.forEach(sensor -> sensor.setActive(false));
		when(securityRepository.getSensors()).thenReturn(listSensor);
		// Image service identifies an image NOT containing a cat
		when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);

		securityService.processImage(mock(BufferedImage.class));

		verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
	}

	@Test
	@DisplayName("9. If the system is disarmed, set the status to no alarm.")
	public void systemDisarmed_verifyStatusChangeToNoAlarm() {
		securityService.setArmingStatus(ArmingStatus.DISARMED);
		verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
	}

	@ParameterizedTest(name = "Test case  using arming status: {0}")
	@EnumSource(value = ArmingStatus.class, names = { "ARMED_AWAY", "ARMED_HOME" })
	@DisplayName("10. If the system is armed, reset all sensors to inactive.")
	public void systemArmed_resetAllSensorsInactive(ArmingStatus armingStatus) {
		when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
		when(securityRepository.getSensors()).thenReturn(listSensor);
		securityService.setArmingStatus(armingStatus);
		securityService.getSensors().forEach(sensor -> assertEquals(false, sensor.getActive()));
	}

	@Test
	@DisplayName("11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.")
	public void systemIsArmedHomeWhilecameraShowCat_veriyChangeAlarmStatusToAlarm() {
		when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

		when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
		
		securityService.processImage(mock(BufferedImage.class));

		verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
	}
	
	
	
	@ParameterizedTest(name = "Test case  using arming status: {0}")
    @EnumSource(value = AlarmStatus.class, names = { "NO_ALARM", "PENDING_ALARM" })
	@DisplayName("12. If the system is disarmed and sensor is activated , make no changes to the alarm state..")
	public void systemIsDisarmed_sensorActivated_veriyNoChangesToArmingState(AlarmStatus status) {
       
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setArmingStatus(ArmingStatus.DISARMED);
    }
	
	@Test
    @DisplayName("13. If alarm status is alarm , set to PENDING ALARM")
    public void alarmStatusIsAlarm_setPendinAlarm() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
		Method getNameMethod = SecurityService.class.getDeclaredMethod("handleSensorDeactivated");
        getNameMethod.setAccessible(true); // Make the private method accessible
        getNameMethod.invoke(securityService);
		verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
	
    @Test
    @DisplayName("14. When adding, getting, and removing sensor , verify exception does not throw")
    public void verifyAddGetRemoveSensor_notThrowException() {
        assertDoesNotThrow(() -> {
        	securityService.addSensor(sensor);
            securityService.getSensors();
            securityService.removeSensor(sensor);
        });
    }
}