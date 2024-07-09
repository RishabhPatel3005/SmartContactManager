package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	@ModelAttribute
	public void addCommonData(Model model,Principal principal) {
		String username = principal.getName();
		User user = userRepository.getUserByUserName(username);
		model.addAttribute("user", user);
	}
	
	@RequestMapping("/dashboard")
	public String dashboard(Model model) {
		
		model.addAttribute("title", "Dashboard - Smart Contact Manager");
		return "normal/user_dashboard";
	}
	
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact - Smart Contact Manager");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file , Principal principal,HttpSession session) {
		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			
			if(file.isEmpty()) {
				System.out.println("File is empty");
				contact.setImage("contact.jpg");
			}
			else {
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("File is uploaded");
			}
			
			contact.setUser(user);
			user.getContacts().add(contact);
			this.userRepository.save(user);
			
			session.setAttribute("message", new Message("Contact Uploaded!! Add More","success"));
			
		} catch (Exception e) {
			e.printStackTrace();
			session.setAttribute("message", new Message("Something Went Wrong!! Try Again","danger"));
		}
		return "normal/add_contact_form";
	}
	
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model m, Principal principal) {
		m.addAttribute("title", "View Contacts - Smart Contact Manager");
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		Pageable pageable = PageRequest.of(page, 3);
		
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(),pageable);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		return "normal/show_contacts";
	}
	
	@GetMapping("/contact-detail/{id}")
	public String showContactDetail(@PathVariable("id") Integer id, Model model, Principal principal) {
		Optional<Contact> optionalContact = this.contactRepository.findById(id);
		Contact contact = optionalContact.get();
		
		String username = principal.getName();
		User user = this.userRepository.getUserByUserName(username);
		
		if(user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact",contact);
			model.addAttribute("title","Contact Detail - Smart Contact Manager");
		}
		else {
			model.addAttribute("title", "Invalid Access - Smart Contact Manager");
		}
		return "normal/contact_detail";
	}
	
	
	@GetMapping("/delete-contact/{id}")
	public String deleteContact(@PathVariable("id") Integer id,Model model,Principal principal,HttpSession session) {
		Optional<Contact> optionalContact = this.contactRepository.findById(id);
		Contact contact = optionalContact.get();
		
		String username = principal.getName();
		User user = this.userRepository.getUserByUserName(username);
		
		if(user.getId() == contact.getUser().getId()) {
			this.contactRepository.delete(contact);
			session.setAttribute("message", new Message("Contact Deleted Successfully!!","success"));
		}
		
		return "redirect:/user/show-contacts/0";
	}
	
	@GetMapping("/update-contact/{id}")
	public String updateContact(@PathVariable("id") Integer id,Model model) {
		
		model.addAttribute("title","Update Contact - Smart Contact Manager");
		Contact contact = this.contactRepository.findById(id).get();
		model.addAttribute("contact",contact);
		
		return "normal/update_contact";
	}
	
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,Model model,HttpSession session,Principal principal) {
		try {
			
			Contact oldContact = this.contactRepository.findById(contact.getcId()).get();
			
			if(!file.isEmpty()) {
				
				//Delete Contact's old Image
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile,oldContact.getImage());
				file1.delete();
				
				
				//Update Contact's new Image
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
				
			}
			else {
				contact.setImage(oldContact.getImage());
			}
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			session.setAttribute("message", new Message("Your contact is updated...","success"));
		}catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println(contact.getName());
		return "redirect:/user/contact-detail/" + contact.getcId();
	}
	
	@GetMapping("/user-profile")
	public String yourProfile(Model model) {
		
		model.addAttribute("title", "Profile Page - Smart Contact Manager");
		return "normal/user_profile";
	}
	
	@GetMapping("/settings")
	public String settings(Model model) {
		model.addAttribute("title","Settings - Smart Contact Manager");
		return "normal/settings";
	}
}
