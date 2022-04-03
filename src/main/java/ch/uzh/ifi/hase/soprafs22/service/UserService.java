package ch.uzh.ifi.hase.soprafs22.service;

import ch.uzh.ifi.hase.soprafs22.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs22.entity.User;
import ch.uzh.ifi.hase.soprafs22.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
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

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setCreationDate(LocalDate.now());

    checkIfUsernameExists(newUser);

    // saves the given entity but data is only persisted in the database once
    // flush() is called
    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }



  public User checkCredentials(User verifiedUser){

      User checkedUser = userRepository.findByUsername(verifiedUser.getUsername());
      String conflictErrorMessage = "add User failed because username already exists!";
      String incorrectErrorMessage = "add User failed because username already exists!";

      if (checkedUser == null ) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                  String.format(conflictErrorMessage));
      }else if(checkedUser.getPassword().equals(verifiedUser.getPassword())){
          return checkedUser;
      }
      else{
          throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                  String.format(incorrectErrorMessage));
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
  private void checkIfUsernameExists(User userToBeCreated) {
    User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

    //changed error msg to match the description
    String baseErrorMessage = "add User failed because username already exists!";
    if (userByUsername != null ) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          String.format(baseErrorMessage));
    }
  }

}
