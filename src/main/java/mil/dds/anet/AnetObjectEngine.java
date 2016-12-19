package mil.dds.anet;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import mil.dds.anet.beans.ApprovalStep;
import mil.dds.anet.beans.Comment;
import mil.dds.anet.beans.Group;
import mil.dds.anet.beans.Organization;
import mil.dds.anet.beans.Person;
import mil.dds.anet.beans.Position;
import mil.dds.anet.beans.Report;
import mil.dds.anet.beans.geo.Location;
import mil.dds.anet.database.AdminDao;
import mil.dds.anet.database.AdminDao.AdminSettingKeys;
import mil.dds.anet.database.ApprovalActionDao;
import mil.dds.anet.database.ApprovalStepDao;
import mil.dds.anet.database.CommentDao;
import mil.dds.anet.database.GroupDao;
import mil.dds.anet.database.IAnetDao;
import mil.dds.anet.database.LocationDao;
import mil.dds.anet.database.OrganizationDao;
import mil.dds.anet.database.PersonDao;
import mil.dds.anet.database.PoamDao;
import mil.dds.anet.database.PositionDao;
import mil.dds.anet.database.ReportDao;
import mil.dds.anet.database.TestingDao;
import mil.dds.anet.views.AbstractAnetBean;
import mil.dds.anet.views.AbstractAnetBean.LoadLevel;

//TODO: change this name
public class AnetObjectEngine {

	TestingDao dao;
	PersonDao personDao;
	GroupDao groupDao;
	PoamDao poamDao;
	LocationDao locationDao;
	OrganizationDao orgDao;
	PositionDao positionDao;
	ApprovalStepDao asDao;
	ApprovalActionDao approvalActionDao;
	ReportDao reportDao;
	CommentDao commentDao;
	AdminDao adminDao;

	private static Map<Class<? extends AbstractAnetBean>, IAnetDao<?>> daoMap;
	private static AnetObjectEngine instance; 
	
	Handle dbHandle;
	
	public AnetObjectEngine(DBI jdbi) { 
		dbHandle = jdbi.open();
		
		personDao = new PersonDao(dbHandle);
		groupDao = new GroupDao(dbHandle);
		poamDao = new PoamDao(dbHandle);
		locationDao =  new LocationDao(dbHandle);
		orgDao = new OrganizationDao(dbHandle, groupDao);
		positionDao = new PositionDao(dbHandle);
		asDao = new ApprovalStepDao(dbHandle);
		approvalActionDao = new ApprovalActionDao(dbHandle);
		reportDao = new ReportDao(dbHandle);
		commentDao = new CommentDao(dbHandle);
		adminDao = new AdminDao(dbHandle);
		
		daoMap = new HashMap<Class<? extends AbstractAnetBean>, IAnetDao<?>>();
		daoMap.put(Person.class, personDao);
		daoMap.put(Group.class, groupDao);
		daoMap.put(Location.class, locationDao);
		daoMap.put(Organization.class, orgDao);
		daoMap.put(Position.class, positionDao);
//		daoMap.put(Poam.class, poamDao);
//		daoMap.put(Tashkil.class, tashkilDao);
//		daoMap.put(ApprovalStep.class, asDao);
//		daoMap.put(ApprovalAction.class, approvalActionDao);
		daoMap.put(Report.class, reportDao);
		daoMap.put(Comment.class, commentDao);
		
		instance = this;
	}
	
	public PersonDao getPersonDao() { 
		return personDao;
	}
	
	public GroupDao groupDao() { 
		return groupDao;
	}
	
	public PoamDao getPoamDao() { 
		return poamDao;
	}

	public GroupDao getGroupDao() {
		return groupDao;
	}

	public LocationDao getLocationDao() {
		return locationDao;
	}

	public OrganizationDao getOrganizationDao() {
		return orgDao;
	}

	public ApprovalActionDao getApprovalActionDao() {
		return approvalActionDao;
	}

	public PositionDao getPositionDao() {
		return positionDao;
	}

	public ApprovalStepDao getApprovalStepDao() {
		return asDao;
	}

	public ReportDao getReportDao() {
		return reportDao;
	}
	
	public CommentDao getCommentDao() { 
		return commentDao;
	}
	
	public AdminDao getAdminDao() { 
		return adminDao;
	}
	
	public Organization getOrganizationForPerson(Person p) { 
		return personDao.getOrganizationForPerson(p.getId());
	}
	
	public List<ApprovalStep> getApprovalStepsForOrg(Organization ao) { 
		Collection<ApprovalStep> unordered = asDao.getByAdvisorOrganizationId(ao.getId());
		
		int numSteps = unordered.size();
		LinkedList<ApprovalStep> ordered = new LinkedList<ApprovalStep>();
		Integer nextStep = null;
		for (int i=0;i<numSteps;i++) { 
			for (ApprovalStep as : unordered) { 
				if (Objects.equals(as.getNextStepId(), nextStep)) { 
					ordered.addFirst(as);
					nextStep = as.getId();
					break;
				}
			}
		}
		return ordered;
	}
	
	public boolean canUserApproveStep(int userId, int approvalStepId) { 
		ApprovalStep as = asDao.getById(approvalStepId);
		Group approvers = groupDao.getById(as.getApproverGroup().getId());
		for (Person member : approvers.getMembers()) { 
			if (member.getId() == userId) { return true; } 
		}
		return false;
	}

	public static AnetObjectEngine getInstance() { 
		return instance;
	}
	
	public static AbstractAnetBean loadBeanTo(AbstractAnetBean bean, LoadLevel ll) {
		@SuppressWarnings("unchecked")
		IAnetDao<? extends AbstractAnetBean> dao = (IAnetDao<? extends AbstractAnetBean>) daoMap.get(bean.getClass());
		if ( dao == null) { throw new UnsupportedOperationException("No dao loaded for " + bean.getClass()); }
		//TODO: support load levels above PROPERTIES. 
		return dao.getById(bean.getId());
	}
	
	public String getAdminSetting(AdminSettingKeys key) { 
		return adminDao.getSetting(key);
	}
}
