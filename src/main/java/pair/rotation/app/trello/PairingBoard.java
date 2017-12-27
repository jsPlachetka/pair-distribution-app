package pair.rotation.app.trello;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.julienvey.trello.domain.Card;
import com.julienvey.trello.domain.TList;
import com.julienvey.trello.impl.TrelloImpl;
import com.julienvey.trello.impl.TrelloUrl;
import com.julienvey.trello.impl.http.RestTemplateHttpClient;

import pair.rotation.app.trello.entities.Company;
import pair.rotation.app.trello.entities.DayPairs;
import pair.rotation.app.trello.entities.Developer;
import pair.rotation.app.trello.entities.Pair;


public class PairingBoard {

	private static final String BUILD_PAIR_LABEL_COLOR = "orange";
	private static final String COMMUNITY_PAIR_LABEL_COLOR = "green";
	private static final String CREATE_LISTS = "/lists?";
    private static final String GET_LIST_CARDS = "/lists/{listId}/cards?";
	private RestTemplateHttpClient httpClient;
	private TrelloImpl trelloImpl;
	private List<Developer> availableDevelopers;
	private List<Developer> allDevelopers;
	private List<Company> allCompanies;
	private List<String> tracks;
	private List<DayPairs> pastPairs;
	private String accessKey;
	private String applicationKey;
	private String pairingBoardId;
    
	public PairingBoard(String accessKey, String applicationKey, String pairingBoardId) {
    		this.accessKey = accessKey;
		this.applicationKey = applicationKey;
		this.pairingBoardId = pairingBoardId;
		availableDevelopers = new ArrayList<Developer>();
		allDevelopers = new ArrayList<>();
		allCompanies = new ArrayList<>();
		httpClient = new RestTemplateHttpClient();
		trelloImpl = new TrelloImpl(applicationKey, accessKey, httpClient);
	}
    
	public List<Developer> getDevs() {
		return allDevelopers.stream().filter(developer -> availableDevelopers.contains(developer)).collect(Collectors.toList());
	}
	
	public List<String> getTracks() {
		return tracks;
	}
	
   public List<DayPairs> getPastPairs() {
	   return pastPairs;
   }

	
	public void syncTrelloBoardState() {
		tracks = new ArrayList<String>();
		pastPairs = new ArrayList<DayPairs>();
		for (TList tList : getLits()) {
			String listName = tList.getName();
			List<Card> cards = getListCards(tList.getId());
			if ("devs".equals(listName.toLowerCase())){
				syncDevs(cards);
				syncDevsMetadata(cards);
			}
			
			if ("tracks".equals(listName.toLowerCase())){
				for (Card card : cards) {
					tracks.add(card.getName());
				}
			}
			
			if (listName.toLowerCase().startsWith("pairing")){
				DayPairs pairs = syncPairs(tList, cards);
				pastPairs.add(pairs);
			}
		}
	}

	private DayPairs syncPairs(TList tList, List<Card> cards) {
		DayPairs pairs = new DayPairs();
		try {
			pairs.setDate(getDateFromCradName(tList.getName()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (Card card : cards) {
			Pair pair = new Pair();
			pair.setBuildPair(isPairWithLabel(card, BUILD_PAIR_LABEL_COLOR));
			pair.setCommunityPair(isPairWithLabel(card, COMMUNITY_PAIR_LABEL_COLOR));
			pair.setDevs( getDevelopersFromCard(card));
			pairs.addPair(card.getName(), pair);
			System.out.println(card.getName());
			System.out.println(card.getDesc());
		}
		return pairs;
	}

	private void syncDevs(List<Card> cards) {
		cards.stream().filter(card -> "devs".equals(card.getName().toLowerCase()))
		              .forEach(card -> availableDevelopers.addAll(getDevelopersFromCard(card)));
	}
	
	private void syncDevsMetadata(List<Card> cards) {
		for (Card card : cards) {
			String lowerCaseCardName = card.getName().toLowerCase();
			if (lowerCaseCardName.startsWith("devops")) {
				String[] companyNames = parseDevOpsCompanies(lowerCaseCardName);
				initDevOpsCompanies(companyNames);
			} else if ("new".equals(lowerCaseCardName)) {
				card.getIdMembers().forEach(developerId -> getDeveloperById(developerId).setNew(true));
			} else {
				setDevsCompany(card.getName(), card);
			}
		}
	}

	protected String[] parseDevOpsCompanies(String lowerCaseCardName) {
		String companies = lowerCaseCardName.replaceFirst("devops:", "");
		if ("".equals(companies)){
			return new String[0];
		}
		return companies.split(",");
	}

	private void initDevOpsCompanies(String[] companyNames) {
		for (String companyName : companyNames) {
			Company company = getCompanyByName(companyName);
			company.setDevOps(true);
		}
	}

	private boolean isPairWithLabel(Card card, String labelColor) {
		return card.getLabels().stream().filter(label -> labelColor.equals(label.getColor()))
				                        .findAny()
				                        .isPresent();
	}
	
	private List<Developer> getDevelopersFromCard(Card card) {
		return card.getIdMembers().stream().map(developerId -> getDeveloperById(developerId)).collect(Collectors.toList());
	}
	
	private void setDevsCompany(String companyName, Card card) {
		Company company = getCompanyByName(companyName);
		card.getIdMembers().stream().map(developerId -> getDeveloperById(developerId)).forEach(developer -> developer.setCompany(company));
	}

	private Company getCompanyByName(String companyName){
		Company result = allCompanies.stream().filter(company -> company.getName().equals(companyName)).findFirst().orElse(null);
		if(result == null){
			result = new Company(companyName);
			allCompanies.add(result);
		}
		
		return result;
	}
	
	private Developer getDeveloperById(String developerId){
		Developer result = allDevelopers.stream().filter(developer -> developer.equals(new Developer(developerId))).findFirst().orElse(new Developer(developerId));
		if(!allDevelopers.contains(result)){
			allDevelopers.add(result);
		}
		
		return result;
	}

	public List<TList> getLits(){
		return trelloImpl.getBoardLists(pairingBoardId);
	}
	
	public Date getDateFromCradName(String name) throws ParseException{
		String date = name.substring(name.indexOf("(") + 1, name.lastIndexOf(")"));
		return new DayPairs().parse(date);
	}
	
	
	public List<Card> getListCards(String listId) {
		TrelloUrl getListCardsURL = TrelloUrl.createUrl(GET_LIST_CARDS);
		List<Card> listCards = Arrays.asList(httpClient.get(getListCardsURL.asString(), Card[].class, listId, applicationKey, accessKey));
		return listCards;
	}

	public void addTodayPairsToBoard(DayPairs pairs, int daysIntoFuture) {
		TList newPairingList = createNewPairingList(pairs, daysIntoFuture);
		newPairingList.setInternalTrello(trelloImpl);		
        addPairsToList(pairs, newPairingList);
	}

	private void addPairsToList(DayPairs pairs, TList newPairingList) {
		for (int i = tracks.size() - 1; i >= 0; i--) {
			if(pairs.getTracks().contains(tracks.get(i))){
				String trackName = tracks.get(i);
				Pair pairByTrack = pairs.getPairByTrack(trackName);
				Card card = new Card();
				card.setName(trackName);
				card.setIdMembers(getIdsFromDevelopers(pairByTrack));
				Card pairingCard = newPairingList.createCard(card);
				if(pairByTrack.isBuildPair()){
					pairingCard.addLabels(BUILD_PAIR_LABEL_COLOR);
				}
				if(pairByTrack.isCommunityPair()){
					pairingCard.addLabels(COMMUNITY_PAIR_LABEL_COLOR);
				}
			}
		}
	}

	private List<String> getIdsFromDevelopers(Pair pair) {
		return pair.getDevs().stream().map(developer -> developer.getId()).collect(Collectors.toList());
	}
	
	private TList createNewPairingList(DayPairs pairs, int daysIntoFuture) {
		TrelloUrl createListURL = TrelloUrl.createUrl(CREATE_LISTS);
		String name = "pairing(" + pairs.format(getFutureDate(pairs.getDate(), daysIntoFuture)) + ")";
		TList tList = new TList();
		tList.setName(name);
		tList.setIdBoard(pairingBoardId);
		createListURL.params();
		TList newPairingList = httpClient.postForObject(createListURL.asString(), tList, TList.class, applicationKey, accessKey);
		return newPairingList;
	}
	
	private Date getFutureDate(Date dateToStart, int daysIntoFuture) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateToStart);
		cal.add(Calendar.DATE, daysIntoFuture);
		return cal.getTime();
	}
}
