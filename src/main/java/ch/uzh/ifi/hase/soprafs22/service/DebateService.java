package ch.uzh.ifi.hase.soprafs22.service;

import ch.uzh.ifi.hase.soprafs22.constant.DebateSide;
import ch.uzh.ifi.hase.soprafs22.constant.DebateState;
import ch.uzh.ifi.hase.soprafs22.entity.DebateRoom;
import ch.uzh.ifi.hase.soprafs22.entity.DebateSpeaker;
import ch.uzh.ifi.hase.soprafs22.entity.DebateTopic;
import ch.uzh.ifi.hase.soprafs22.entity.User;
import ch.uzh.ifi.hase.soprafs22.repository.*;
import ch.uzh.ifi.hase.soprafs22.rest.dto.DebateRoomPostDTO;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static ch.uzh.ifi.hase.soprafs22.entity.DebateTopic.readTopicListCSV;

@Service
@Transactional
public class DebateService {

    private final Logger log = LoggerFactory.getLogger(DebateService.class);

    private final DebateTopicRepository debateTopicRepository;
    private final DebateRoomRepository debateRoomRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final DebateSpeakerRepository debateSpeakerRepository;
    private final UserService userService;

    @Autowired
    public DebateService(
            @Qualifier("debateTopicRepository") DebateTopicRepository debateTopicRepository,
            @Qualifier("debateRoomRepository") DebateRoomRepository debateRoomRepository,
            @Qualifier("userRepository") UserRepository userRepository,
            @Qualifier("debateSpeakerRepository") DebateSpeakerRepository debateSpeakerRepository,
            @Qualifier("tagRepository") TagRepository tagRepository,
            @Qualifier("userService") UserService userService
            ) {

        this.debateTopicRepository = debateTopicRepository;
        this.debateRoomRepository = debateRoomRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.debateSpeakerRepository = debateSpeakerRepository;
        this.userService = userService;
    }

    @PostConstruct
    private void setupDefaultDebateTopics(){
        log.info("Setup default debate topics");

        // Check if the debate repository is empty
        if (debateTopicRepository.count() != 0L){
            log.info("Default debate topics already created");
            return;
        }

        // If it is not empty load the default topics from file
        Path defaultListPath = Paths.get("setup", "defaultTopics.csv");

        if (log.isDebugEnabled()) { log.info(String.format("Loading default topic list from: %s", defaultListPath)); }

        try {
            List<DebateTopic> defaultDebateTopicsList = readTopicListCSV(defaultListPath.toString());
            if (defaultDebateTopicsList.isEmpty()){
                log.warn("List of debate topics is empty");
            } else{
                debateTopicRepository.saveAll(defaultDebateTopicsList);
                if (log.isDebugEnabled()) { log.info(String.format("Default Topics created %d", defaultDebateTopicsList.size())); }
            }
        } catch(IOException | CsvValidationException e){
            log.error("Problem loading the default file list");
        }
    }


    public DebateRoom createDebateRoom(DebateRoom inputDebateRoom, DebateRoomPostDTO debateRoomPostDTO) {

        // Check that the debate topic exists and add it
        Optional<DebateTopic> debateTopic = debateTopicRepository.findById(debateRoomPostDTO.getDebateId());

        if (debateTopic.isEmpty()){
            String baseErrorMessage = "Error: reason <Debate topic with id: '%d' was not found>";
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format(baseErrorMessage, debateRoomPostDTO.getDebateId()));
        }

        inputDebateRoom.setDebateTopic(debateTopic.get());

        // Set the state of the debate
        if (debateRoomPostDTO.getSide() == DebateSide.FOR){
            inputDebateRoom.setDebateRoomStatus(DebateState.ONE_USER_FOR);
        } else{
            inputDebateRoom.setDebateRoomStatus(DebateState.ONE_USER_AGAINST);
        }

        // Check that user that will create the debate exists and add it as a Speaker to the debate room
        Optional<User> creatingUser = userRepository.findById(debateRoomPostDTO.getUserId());

        if (creatingUser.isEmpty()){
            String baseErrorMessage = "Error: reason <User with id: '%d' was not found>";
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format(baseErrorMessage, debateRoomPostDTO.getUserId()));
        }

        DebateSpeaker debatesSpeaker1 = new DebateSpeaker();
        debatesSpeaker1.setUserAssociated(creatingUser.get());
        debatesSpeaker1.setDebateSide(debateRoomPostDTO.getSide());

        debatesSpeaker1.setDebateRoom(inputDebateRoom);
        inputDebateRoom.setUser1(debatesSpeaker1);

        // Store new DebateRoom in the DB
        inputDebateRoom = debateRoomRepository.save(inputDebateRoom);
        debateRoomRepository.flush();

        debateSpeakerRepository.save(debatesSpeaker1);
        debateSpeakerRepository.flush();

        log.debug("Created DebateRoom: {}", inputDebateRoom);

        return inputDebateRoom;

    }

    public DebateRoom getDebateRoom(Long roomId) {
        return debateRoomRepository.findByRoomId(roomId);
    }

    public List<DebateTopic> getDebateTopicByUserId(Long userId){

        User creatorUser = userRepository.findById(userId).orElse(null);
        String baseErrorMessage = "Error: reason <Can not get topics because User with id: '%d' was not found>";

        if(creatorUser == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(baseErrorMessage,userId));
        }

        List<DebateTopic> debateTopicList = debateTopicRepository.findByIsDefaultTopicIsTrue();
        debateTopicList.addAll(debateTopicRepository.findByCreatorUserId(userId));
        return debateTopicList;
    }

    public List<DebateRoom> getDebateRooms() {
        return this.debateRoomRepository.findAll();
    }

    public DebateRoom deleteRoom(Long roomID){

        DebateRoom roomToDelete = debateRoomRepository.findByRoomId(roomID);

        if(roomToDelete == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        else{
            List<DebateSpeaker> occupiedDebateRooms = debateSpeakerRepository.findAllByDebateRoom(roomToDelete);

            if(!occupiedDebateRooms.isEmpty()){
                debateSpeakerRepository.deleteAll(occupiedDebateRooms);
                debateSpeakerRepository.flush();
            }
            debateRoomRepository.delete(roomToDelete);
            debateRoomRepository.flush();
        }
        return roomToDelete;
    }

    public DebateRoom addParticipantToRoom(DebateRoom actualRoom, User userToAdd){

        User checkUser = userRepository.findByid(userToAdd.getId());

        if(checkUser == null){
            User guestUser = new User();
            checkUser = userService.createGuestUser(guestUser);
        }

        DebateRoom updatedRoom = debateRoomRepository.findByRoomId(actualRoom.getRoomId());

        DebateSpeaker debatesSpeaker = new DebateSpeaker();
        debatesSpeaker.setUserAssociated(checkUser);

        debatesSpeaker.setDebateRoom(updatedRoom);
        updatedRoom.setUser2(debatesSpeaker);

        if(updatedRoom.getSpeakers().get(0).getDebateSide() == DebateSide.FOR){
            debatesSpeaker.setDebateSide(DebateSide.AGAINST);
        }
        else{
            debatesSpeaker.setDebateSide(DebateSide.FOR);
        }

        updatedRoom.setDebateRoomStatus(DebateState.READY_TO_START);

        debateRoomRepository.save(updatedRoom);
        debateRoomRepository.flush();

        debateSpeakerRepository.save(debatesSpeaker);
        debateSpeakerRepository.flush();

        log.debug("Participant added to the DebateRoom: {}", updatedRoom);

        return updatedRoom;
    }

    public DebateRoom setStatus(Long roomID, Integer status){

        DebateRoom updatedRoom = debateRoomRepository.findByRoomId(roomID);
        DebateState[] toSet = DebateState.values();
        updatedRoom.setDebateRoomStatus(toSet[status]);

        debateRoomRepository.save(updatedRoom);
        debateRoomRepository.flush();

        log.debug("Status Set to the DebateRoom: {}", updatedRoom);

        return updatedRoom;
    }

}
