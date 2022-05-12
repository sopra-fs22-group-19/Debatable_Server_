package ch.uzh.ifi.hase.soprafs22.controller;

import ch.uzh.ifi.hase.soprafs22.entity.*;
import ch.uzh.ifi.hase.soprafs22.entity.DebateRoom;
import ch.uzh.ifi.hase.soprafs22.entity.DebateTopic;
import ch.uzh.ifi.hase.soprafs22.entity.Intervention;
import ch.uzh.ifi.hase.soprafs22.entity.User;
import ch.uzh.ifi.hase.soprafs22.rest.dto.*;
import ch.uzh.ifi.hase.soprafs22.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs22.service.DebateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@RestController
public class DebateController {


    @Value("${api.key}")
    private String apikey;

    @Value("${api.host}")
    private String host;
    private final DebateService debateService;

    DebateController(DebateService debateService) {
        this.debateService = debateService;
    }

    @PostMapping("/debates/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public DebateRoomGetDTO createDebateRoom(@RequestBody DebateRoomPostDTO debateRoomPostDTO) {
        // convert API user to internal representation
        DebateRoom inputDebateRoom = DTOMapper.INSTANCE.convertDebateRoomPostDTOtoEntity(debateRoomPostDTO);

             // Create the debate room in the DB
        DebateRoom createdDebateRoom = debateService.createDebateRoom(inputDebateRoom, debateRoomPostDTO);

        // convert internal representation of user back to API
        return DTOMapper.INSTANCE.convertEntityToDebateRoomGetDTO(createdDebateRoom);
    }


    @GetMapping("/debates/rooms/{roomId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public DebateRoomGetDTO getDebateRoom(@PathVariable Long roomId) {
        String errorMessage = String.format("Error: <the Debate Room with id: '%d' was not found>",  roomId);
        DebateRoom debateRoom = debateService.getDebateRoom(roomId, errorMessage);

        return DTOMapper.INSTANCE.convertEntityToDebateRoomGetDTO(debateRoom);

    }

    @GetMapping("/debates/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DebateTopicGetDTO> getTopicByUser(@PathVariable Long userId){

        List<DebateTopicGetDTO> debateGetDTOs = new ArrayList<>();
        List<DebateTopic> debateTopics = debateService.getDebateTopicByUserId(userId);

        for (DebateTopic debateTopic : debateTopics) {
            debateGetDTOs.add(DTOMapper.INSTANCE.convertEntityToDebateGetDTO(debateTopic));
        }

        return debateGetDTOs;
    }

    @PostMapping("/debates")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public DebateTopicGetDTO postDebateTopic(@RequestBody DebateTopicPostDTO debateTopicPostDTO) {

        DebateTopic newDebateTopic = DTOMapper.INSTANCE.convertDebateTopicPostDTOtoEntity(debateTopicPostDTO);
        newDebateTopic = debateService.createDebateTopic(debateTopicPostDTO.getUserId(), newDebateTopic);

        // Get interventions of user specified
        return DTOMapper.INSTANCE.convertEntityToDebateGetDTO(newDebateTopic);
    }

    @DeleteMapping("/debates/rooms/{roomId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void deleteDebateRoomById(@PathVariable("roomId") Long roomId){
        debateService.deleteRoom(roomId);
    }

    @PutMapping("/debates/rooms/{roomId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public DebateRoomGetDTO addSecondParticipantById(@PathVariable("roomId") Long roomId, @RequestBody UserPutDTO userPutDTO){

        DebateRoom toUpdateRoom = new DebateRoom();
        toUpdateRoom.setRoomId(roomId);
        User userToAdd = DTOMapper.INSTANCE.convertUserPutDTOtoEntity(userPutDTO);
        DebateRoom updatedRoom = debateService.addParticipantToRoom(toUpdateRoom, userToAdd);

        return DTOMapper.INSTANCE.convertEntityToDebateRoomGetDTO(updatedRoom);
    }

    @PutMapping("/debates/rooms/{roomId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public DebateRoomGetDTO updateStatus(@PathVariable("roomId") Long roomId,
                                         @RequestBody DebateRoomStatusPutDTO debateRoomStatusPutDTO){

        DebateRoom debateRoom = DTOMapper.INSTANCE.convertDebateRoomStatusPutDTOtoEntity(debateRoomStatusPutDTO);

        DebateRoom updatedRoom = debateService.setStatus(roomId, debateRoom);

        return DTOMapper.INSTANCE.convertEntityToDebateRoomGetDTO(updatedRoom);
    }

    @PostMapping("/debates/rooms/{roomId}/msg")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public void postMessage(@PathVariable("roomId") Long roomId, @RequestBody InterventionPostDTO interventionPostDTO) {
        // convert API user to internal representation
        Intervention inputIntervention = DTOMapper.INSTANCE.convertInterventionPostDTOtoEntity(interventionPostDTO);

        // Create the intervention in the DB
        debateService.createIntervention(inputIntervention, interventionPostDTO);

        // convert internal representation of user back to API
    }

    @GetMapping("/debates/rooms/{roomId}/users/{userId}/msgs")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public List<String> getMessages(@PathVariable("roomId") Long roomId, @PathVariable("userId") Long userId,
                                    @RequestParam(name = "top_i", required = false) Integer topI,
                                    @RequestParam(name = "to_top_j", required = false) Integer toTopJ) {

        // Get interventions of user specified
        return debateService.getUserDebateInterventions(roomId, userId, topI, toTopJ);
    }

    @GetMapping("/debates/")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<DebateTopicGetDTO> getSelectedCategories(@RequestParam(name = "categories") String categories){

        List<DebateTopic> toConvert = debateService.getDebateTopicByCategories(categories);
        List<DebateTopicGetDTO> toSend = new ArrayList<>();

        for (DebateTopic debateTopic : toConvert) {
            toSend.add(DTOMapper.INSTANCE.convertEntityToDebateGetDTO(debateTopic));
        }

        return toSend;
    }

    //for dev and test only
    @GetMapping("/api/usage")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String getApiUsage() {
        RestTemplate restTemplate = new RestTemplate();

        ApiUsage usage = restTemplate.getForObject(host + "usage?auth_key=" + apikey, ApiUsage.class);

        return usage.toString();
    }

    @GetMapping("/translate")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String getTranslation(@RequestParam String msg , @RequestParam String target_lang) {
        //https://api-free.deepl.com/v2/translate?auth_key=8b834a25-0b9a-7cca-5c37-148e566acbeb:fx&text=hello,world&target_lang=DE
        RestTemplate restTemplate = new RestTemplate();
        String text = "&text="+msg;
        String targetLang = "&target_lang="+target_lang;

        TranslationResponse translationResponse = restTemplate.getForObject(host + "translate?auth_key=" + apikey + text + targetLang, TranslationResponse.class);


        String msgTranslated = translationResponse.getTranslations().getText();


        return msgTranslated;
    }

}
