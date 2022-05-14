package ch.uzh.ifi.hase.soprafs22.service;

import ch.uzh.ifi.hase.soprafs22.config.PasswordConfig;
import ch.uzh.ifi.hase.soprafs22.entity.User;
import ch.uzh.ifi.hase.soprafs22.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;


  private PasswordConfig passwordConfig;

  @Autowired
  public UserService(
          @Qualifier("userRepository") UserRepository userRepository,
          @Qualifier("passwordConfig") PasswordConfig passwordConfig
          ) {
    this.userRepository = userRepository;
    this.passwordConfig = passwordConfig;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setCreationDate(LocalDate.now());
    newUser.setEncodedPw(passwordConfig.passwordEncoder().encode(newUser.getPassword()));

    checkIfUsernameExists(newUser);

    // saves the given entity but data is only persisted in the database once
    // flush() is called
    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  public User createGuestUser(User newUser) {

      newUser.setUsername(UUID.randomUUID().toString());
      newUser.setName("Guest");
      newUser.setPassword(UUID.randomUUID().toString());
      newUser.setToken(UUID.randomUUID().toString());
      newUser.setCreationDate(LocalDate.now());
      checkIfUsernameExists(newUser);

      // saves the given entity but data is only persisted in the database once
      // flush() is called
      newUser = userRepository.save(newUser);
      userRepository.flush();

      log.debug("Created Information for GuestUser: {}", newUser);
      return newUser;
  }

  public User checkCredentials(String username, String password){

      User checkedUser = userRepository.findByUsername(username);

      if(checkedUser == null){
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The provided username is invalid.");
      }

      if(checkedUser.getPassword().equals(password)){
          return checkedUser;
      }
      else{
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The password provided is incorrect");
      }
  }

  public void deleteUser(Long id) {
      this.userRepository.deleteById(id);
  }

  public User getUserByUserId(Long userId, String errorMessage){

      errorMessage = String.format("Error: reason <%s>", errorMessage);
      User user = userRepository.findByid(userId);

      if(user == null)
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage);

      return user;
  }

  public User updateUser(Long id, User userDetails){

      String errorMessage = String.format("User with id: '%d' was not found", id);
      String errorMessagePassword = "The password must be different from the previous one.";
      String errorMessageUsername = "The selected username is already taken by another user, choose another username.";

      User toUpdateUser = userRepository.findByid(id);

      if(Objects.isNull(toUpdateUser)){
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage);
      }

      if(Objects.equals(userDetails.getPassword(), toUpdateUser.getPassword())){
          throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessagePassword);
      }

      else{
          if(!Objects.isNull(userDetails.getName())){
              if(!userDetails.getName().isEmpty()){
                  toUpdateUser.setName(userDetails.getName());
              }
          }
          if(!Objects.isNull(userDetails.getUsername())){
              if(!userDetails.getUsername().isEmpty() && !Objects.equals(userDetails.getUsername(), toUpdateUser.getUsername())){
                  checkIfUsernameExists(userDetails);
                  toUpdateUser.setUsername(userDetails.getUsername());
              }
          }
          if(!Objects.isNull(userDetails.getPassword())){
              if(!userDetails.getPassword().isEmpty()){
                  toUpdateUser.setPassword(userDetails.getPassword());
              }
          }
          userRepository.saveAndFlush(toUpdateUser);

          return toUpdateUser;
      }
  }




  /**
   * This is a helper method that will check the uniqueness criteria of the
   * username and the name
   * defined in the User entity. The method will do nothing if the input is unique
   * and throw an error otherwise.
   *
   * @param userToBeCreated
   * @throws org.springframework.web.server.ResponseStatusException
   * @see User
   */


  // changed to only check if username is unique, template check both username and name
  public void checkIfUsernameExists(User userToBeCreated) {
    User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

    if (userByUsername != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "The username provided is not unique. Therefore, the user could not be created!");

    }
  }

}
