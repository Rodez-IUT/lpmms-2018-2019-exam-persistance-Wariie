package ourbusinessproject;

import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
public class EnterpriseProjectService {

    @PersistenceContext
    private EntityManager entityManager;

    public Project saveProjectForEnterprise(Project project, Enterprise enterprise) {
        Enterprise e = saveEnterprise(enterprise);
        if(project.getEnterprise() != null) {
        	project.getEnterprise().getProjects().remove(project);
        }
        project.setEnterprise(e);
        if(!entityManager.contains(project)) {
        	Project p  = entityManager.merge(project);
        	entityManager.persist(p);
        	project = p;
        } else {
        	entityManager.persist(project);
        }
        e.addProject(project);
        entityManager.flush();
        return project;
    }

    public Enterprise saveEnterprise(Enterprise enterprise) {
    	Enterprise e =  entityManager.merge(enterprise);
    	if(e.getProjects()!=null) {
    		for(Project p : e.getProjects()) {
    			if(entityManager.contains(p)) {
    				Project proj = entityManager.merge(p);
    				entityManager.persist(proj);
    				proj.setEnterprise(e);
    				//System.err.println("Test1");
    			} /*else {
    				entityManager.merge(p);
    				p.setEnterprise(e);
    				System.err.println("Test2");
    			} */
    		}
    	}
        entityManager.flush();
        return e;
    }

    public Project findProjectById(Long id) {
        return entityManager.find(Project.class, id);
    }

    public Enterprise findEnterpriseById(Long id) {
        return entityManager.find(Enterprise.class, id);
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;                                                                                                                                                                                                                                                                                                                                
    }
    

    public List<Project> findAllProjects() {
        TypedQuery<Project> query = entityManager.createQuery("select p from Project p join fetch p.enterprise order by p.title", Project.class);
        return query.getResultList();
    }
}
