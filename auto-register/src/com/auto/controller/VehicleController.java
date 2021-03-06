package com.auto.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.auto.entity.Vehicle;
import com.auto.entity.VehiclePersonHelper;
import com.auto.service.LogService;
import com.auto.service.VehicleService;

@Controller
@RequestMapping("/vehicle")
public class VehicleController {

	@Autowired
	private VehicleService vehicleService;
	@Autowired
	private LogService logService;

	@InitBinder
	public void initBinder(WebDataBinder dataBinder) {
		// StringTrimerEditor removes whitespace leading and trailing
		StringTrimmerEditor stringTrimmerEditor = new StringTrimmerEditor(true);
		dataBinder.registerCustomEditor(String.class, stringTrimmerEditor);
	}

	@GetMapping("/list")
	public String listVehicles(Model theModel) {

		List<Vehicle> theVehicles = vehicleService.getVehicles();

		theModel.addAttribute("vehiclesForTheJsp", theVehicles);

		return "list-vehicles";
	}

	@GetMapping("/showFormForAddingVehicle")
	public String showAddVehicleForm(Model theModel) {
		theModel.addAttribute("vehicleXperson", new VehiclePersonHelper());
		return "add-vehicle";
	}

	@PostMapping("/addVehicle")
	public String processForm(@Valid @ModelAttribute("vehicleXperson") VehiclePersonHelper theVehicleXperson,
			BindingResult theBindingResult) {

		if (theBindingResult.hasErrors()) {
			return "add-vehicle";
		} else {
			// Check if CNP exists in the database, if not return error, an anonymus user
			// needs to have an appoinment to register the car
			if (vehicleService.checkIfPersonExists(theVehicleXperson.getUniqueNumberForPerson()) == false) {
				return "error";
			}
			VehiclePersonHelper theOldVehicleXperson = null;
			if (theVehicleXperson.getVehicleId() != 0) {
				theOldVehicleXperson = vehicleService.getVehicleAndOwnerFromDatabase(theVehicleXperson.getVehicleId());
			}
			// Add vehicle to database
			vehicleService.addVehicleToDatabase(theVehicleXperson);

			if (theVehicleXperson.getVehicleId() != 0) {
				String logType = "UPDATE";
				String logMessage = "Updated: (OLD VALUE) " + theOldVehicleXperson.toString() + "\n (NEW VALUE) "
						+ theVehicleXperson.toString();
				logService.addLogToDatabase(logType, logMessage);
			}

			return "vehicle-confirmation";
		}
	}

	@GetMapping("/delete")
	public String deleteVehicle(@RequestParam("vehicleIdToDelete") int vehicleId) {

		VehiclePersonHelper theVehicleXperson = vehicleService.getVehicleAndOwnerFromDatabase(vehicleId);
		vehicleService.deleteVehicleFromDatabase(vehicleId);
		String logType = "DELETE";
		String logMessage = "Deleted: " + theVehicleXperson.toString();
		logService.addLogToDatabase(logType, logMessage);
		return "redirect:/vehicle/list";
	}

	@GetMapping("/showFormForUpdate")
	public String showUpdateVehicleForm(@RequestParam("vehicleIdToUpdate") int vehicleId, Model theModel) {

		// get the vehicle from database
		// get the CNP of the owner after you have the person_id from vehicle
		VehiclePersonHelper theVehicleXperson = vehicleService.getVehicleAndOwnerFromDatabase(vehicleId);
		theModel.addAttribute("vehicleXperson", theVehicleXperson);
		return "add-vehicle";
	}

}
