package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import javax.servlet.ServletContext;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFormContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutablePersonaContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.dotcms.contenttype.transform.contenttype.FromStructureTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.test.DataSourceForTesting;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ContentTypeFactoryImplTest {

	final ContentTypeFactory factory = new ContentTypeFactoryImpl();

	@BeforeClass
	public static void initDb() throws DotDataException, Exception {

		DotConnect dc = new DotConnect();
		String structsToDelete = "(select inode from structure where structure.velocity_var_name like 'velocityVarNameTesting%' )";

		dc.setSQL("delete from field where structure_inode in " + structsToDelete);
		dc.loadResult();

		dc.setSQL("delete from inode where type='field' and inode not in  (select inode from field)");
		dc.loadResult();

		dc.setSQL("delete from contentlet_version_info where identifier in (select identifier from contentlet where structure_inode in "
				+ structsToDelete + ")");
		dc.loadResult();

		dc.setSQL("delete from contentlet where structure_inode in " + structsToDelete);
		dc.loadResult();

		dc.setSQL("delete from inode where type='contentlet' and inode not in  (select inode from contentlet)");
		dc.loadResult();

		dc.setSQL("delete from structure where  structure.velocity_var_name like 'velocityVarNameTesting%' ");
		dc.loadResult();

		dc.loadResult();
		dc.setSQL("delete from inode where type='structure' and inode not in  (select inode from structure)");
		dc.loadResult();

		dc.setSQL("delete from field where structure_inode not in (select inode from structure)");
		dc.loadResult();

		dc.setSQL("delete from inode where type='field' and inode not in  (select inode from field)");
		dc.loadResult();

		dc.setSQL("update structure set structure.url_map_pattern =null, structure.page_detail=null where structuretype =3");
		dc.loadResult();

	}

	@Test
	public void testDifferentContentTypes() throws Exception {

		ContentType content = factory.find(Constants.CONTENT);
		ContentType news = factory.find(Constants.NEWS);
		ContentType widget = factory.find(Constants.WIDGET);
		ContentType form = factory.find(Constants.FORM);
		ContentType fileAsset = factory.find(Constants.FILEASSET);
		ContentType htmlPage = factory.find(Constants.HTMLPAGE);
		ContentType persona = factory.find(Constants.PERSONA);

		// Test all the types
		assertThat("ContentType is type Content", content.baseType() == BaseContentType.CONTENT);
		assertThat("ContentType is type Content", content instanceof ImmutableSimpleContentType);
		assertThat("News is not simple content", !news.equals(content));

		assertThat("ContentType is type FILEASSET", fileAsset.baseType() == BaseContentType.FILEASSET);
		assertThat("ContentType is type FILEASSET", fileAsset instanceof ImmutableFileAssetContentType);
		assertThat("ContentType is type WIDGET", widget.baseType() == BaseContentType.WIDGET);
		assertThat("ContentType is type WIDGET", widget instanceof ImmutableWidgetContentType);

		assertThat("ContentType is type FORM", form.baseType() == BaseContentType.FORM);
		assertThat("ContentType is type FORM", form instanceof ImmutableFormContentType);
		assertThat("ContentType is type PERSONA", persona.baseType() == BaseContentType.PERSONA);
		assertThat("ContentType is type PERSONA", persona instanceof ImmutablePersonaContentType);
		assertThat("ContentType is type HTMLPAGE", htmlPage.baseType() == BaseContentType.HTMLPAGE);
		assertThat("ContentType is type HTMLPAGE", htmlPage instanceof ImmutablePageContentType);

	}

	@Test
	public void testFindMethodEquals() throws Exception {
		List<ContentType> types = factory.findAll();
		for (ContentType type : types) {
			ContentType contentType = factory.find(type.inode());
			ContentType contentType2 = factory.findByVar(type.velocityVarName());
			try {
				assertThat("ContentType == ContentType2", contentType.equals(contentType2) && contentType.equals(type));
			} catch (Throwable t) {

				throw t;
			}
		}
	}
	@Test
	public void testFindUrlMapped() throws Exception {
		List<ContentType> types = factory.findUrlMapped();
		assertThat("findUrlMapped only returns urlmapped content", types.size()>0);
		for(ContentType type : types){
			assertThat("findUrlMapped only returns urlmapped content", type.urlMapPattern()!=null);
		}
		
	}
	
	
	@Test
	public void testFindAll() throws Exception {
		List<ContentType> types = factory.findAll();
		assertThat("findAll sort by Name has same size as find all", factory.findAll("name").size() == types.size());
	}

	@Test
	public void testFieldsMethod() throws Exception {

		Cache<String, ContentType> cacheTest = CacheBuilder.newBuilder().maximumSize(100).build();

		ContentType type = factory.find(Constants.NEWS);
		cacheTest.put(type.inode(), type);
		System.out.println(type);

		ContentType otherType = cacheTest.getIfPresent(type.inode());

		List<Field> fields = otherType.fields();
		System.out.println(type);
		List<Field> fields2 = APILocator.getFieldAPI2().byContentType(type);
		assertThat("We have fields!", fields.size() > 0 && fields.size() == fields2.size());
		for (int j = 0; j < fields.size(); j++) {
			Field field = fields.get(j);
			Field testField = fields2.get(j);
			assertThat("fields are correct:", field.equals(testField));
		}

	}

	@Test
	public void testSerialization() throws Exception {

		File temp = File.createTempFile("test1", "obj");
		File temp2 = File.createTempFile("test2", "obj");
		ContentType origType = factory.find(Constants.NEWS);
		
		try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(temp))){
			oos.writeObject(origType);
			oos.close();
		}

		temp.renameTo(temp2);
		ContentType fromDisk = null;
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(temp2))){
			fromDisk = (ContentType) ois.readObject();
			ois.close();
		}
		

		try {
			assertThat("fields are correct:", origType.equals(fromDisk));
		} catch (Throwable e) {
			System.out.println("origType" + origType);
			System.out.println("fromDisk" + fromDisk);
			throw e;
		}

		List<Field> fields = origType.fields();
		List<Field> fields2 = fromDisk.fields();

		assertThat("We have fields!", fields.size() > 0 && fields.size() == fields2.size());
		for (int j = 0; j < fields.size(); j++) {
			Field field = fields.get(j);
			Field testField = fields2.get(j);
			assertThat("fields are correct:", field.equals(testField));
		}

	}

	@Test
	public void testLegacyTransform() throws Exception {
		
		Structure st = new Structure();
		List<ContentType> types = factory.findAll("name");
		List<ContentType> oldTypes = new FromStructureTransformer(StructureFactory.getStructures()).asList();

		assertThat("findAll and legacy return same quantity", types.size() == oldTypes.size());

		for (int i = 0; i < types.size(); i++) {
			try {
				assertThat("Old and New Contentyypes are the same", types.get(i).equals(oldTypes.get(i)));
			} catch (Throwable t) {
				System.out.println("Old and New Contentyypes are NOT the same");
				System.out.println(types.get(i));
				System.out.println(oldTypes.get(i));
				throw t;
			}
		}

		assertThat("findAll sort by Name has same size as find all", factory.findAll("name").size() == types.size());
	}

	@Test
	public void testAddingContentTypes() throws Exception {
		int count = factory.searchCount(null);
		int runs = 20;

		for (int i = 0; i < runs; i++) {
			long time = System.currentTimeMillis() + i;
			int base = (i % 5) + 1;
			Thread.sleep(1);
			ContentType type = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(base)).description("description" + time)
					.folder(FolderAPI.SYSTEM_FOLDER).host(Constants.SYSTEM_HOST).name("ContentTypeTesting" + time).owner("owner")
					.velocityVarName("velocityVarNameTesting" + time).build();
			type = factory.save(type);
		}
		int count2 = factory.searchCount(null);
		assertThat("contenttypes are added", count == count2 - runs);
	}

	
	@Test public void testDefaultType() throws DotDataException{
		ContentType type = factory.findDefaultType();
		assertThat("we have a default content type", type !=null);

	}
	
	
	
	@Test
	public void testSearch() throws Exception {
		String[] searchTerms = { Constants.NEWS, "news", "content", "structuretype = 2", " And structure.inode='" + Constants.NEWS + "'" };

		int totalCount = factory.searchCount(null);

		List<ContentType> types;
		assertThat("we have at least 40 content types", factory.search(null, BaseContentType.ANY, "name", 0, 100).size() > 40);
		types = factory.search(null, BaseContentType.ANY, "name", 0, 5);
		assertThat("limit works and we have max five content types", types.size() < 6);
		for (int x = 0; x < totalCount; x = x + 5) {
			types = factory.search(null, BaseContentType.ANY, "name", x, 5);
			assertThat("we have max five content types", types.size() < 6);
		}

		for (int i = 0; i < BaseContentType.values().length; i++) {
			types = factory.search(null, BaseContentType.getBaseContentType(i), "name", 0, 1000);
			assertThat("we have content types of" + BaseContentType.getBaseContentType(i), types.size() > 0);
			int count = factory.searchCount(null,  BaseContentType.getBaseContentType(i));
			assertThat("Count works as well", types.size() == count);
		}

		for (int i = 0; i < searchTerms.length; i++) {
			types = factory.search(searchTerms[i], BaseContentType.ANY, "mod_date desc", 0, 1000);
			assertThat("we can search content types:" + searchTerms[i], types.size() > 0);
			int count = factory.searchCount(searchTerms[i],  BaseContentType.ANY);
			assertThat("Count works as well", types.size() == count);
		}

	}

	@Test
	public void testAddingWidgets() throws Exception {

		int countAll = factory.searchCount(null);
		int runs = 20;
		int countWidgets = factory.searchCount(null, BaseContentType.WIDGET);

		for (int i = 0; i < runs; i++) {
			addWidget();
			Thread.sleep(1);
		}

		int countAll2 = factory.searchCount(null);
		int countWidgets2 = factory.searchCount(null, BaseContentType.WIDGET);
		assertThat("counts are working", countAll == countAll2 - runs);
		assertThat("counts are working", countAll2 > countWidgets2);
		assertThat("counts are working", countWidgets == countWidgets2 - runs);

	}

	@Test
	public void testAddingPersonas() throws Exception {

		int countAll = factory.searchCount(null);
		int runs = 20;
		int countPersonas = factory.searchCount(null, BaseContentType.PERSONA);

		for (int i = 0; i < runs; i++) {
			addPersona();
			Thread.sleep(1);
		}

		int countAll2 = factory.searchCount(null);
		int countPersonas2 = factory.searchCount(null, BaseContentType.PERSONA);
		assertThat("counts are working", countAll == countAll2 - runs);
		assertThat("counts are working", countAll2 > countPersonas2);
		assertThat("counts are working", countPersonas == countPersonas2 - runs);

	}

	@Test
	public void testAddingFileAssets() throws Exception {

		int countAll = factory.searchCount(null);
		int runs = 20;
		int countFiles = factory.searchCount(null, BaseContentType.FILEASSET);

		for (int i = 0; i < runs; i++) {
			addFileAsset();
			Thread.sleep(1);
		}

		int countAll2 = factory.searchCount(null);
		int countPersonas2 = factory.searchCount(null, BaseContentType.FILEASSET);
		assertThat("counts are working", countAll == countAll2 - runs);
		assertThat("counts are working", countAll2 > countPersonas2);
		assertThat("counts are working", countFiles == countPersonas2 - runs);

	}

	@Test
	public void testAddingForms() throws Exception {

		int countAll = factory.searchCount(null);
		int runs = 20;
		int countForms = factory.searchCount(null, BaseContentType.FORM);

		for (int i = 0; i < runs; i++) {
			addForm();
			Thread.sleep(1);
		}

		int countAll2 = factory.searchCount(null);
		int countForms2 = factory.searchCount(null, BaseContentType.FORM);
		assertThat("counts are working", countAll == countAll2 - runs);
		assertThat("counts are working", countAll2 > countForms2);
		assertThat("counts are working", countForms == countForms2 - runs);

	}

	public void addWidget() throws DotDataException {

		long i = System.currentTimeMillis();

		ContentType type = ImmutableWidgetContentType.builder().description("description" + i).expireDateVar(null)
				.folder(FolderAPI.SYSTEM_FOLDER).host(Constants.SYSTEM_HOST).name("widgetTesting" + i).owner("owner")

				.velocityVarName("velocityVarNameTesting" + i).build();
		type = factory.save(type);

		List<Field> fields = new FieldFactoryImpl().byContentTypeId(type.inode());
		List<Field> baseTypeFields = ImmutableWidgetContentType.builder().name("test").velocityVarName("rewarwa").build().requiredFields();
		assertThat("fields are all added", fields.size() == baseTypeFields.size());

		for (int j = 0; j < fields.size(); j++) {
			Field field = fields.get(j);
			Field baseField = baseTypeFields.get(j);
			assertThat("fields are correct:", field.dataType().equals(baseField.dataType()));
			assertThat("fields are correct:", field.variable().equals(baseField.variable()));
			assertThat("fields are correct:", field.getClass().equals(baseField.getClass()));
			assertThat("fields are correct:", field.name().equals(baseField.name()));
			assertThat("fields are correct:", field.sortOrder() == baseField.sortOrder());
		}
	}

	public void addForm() throws DotDataException {

		long i = System.currentTimeMillis();

		ContentType type = ImmutableFormContentType.builder().description("description" + i).expireDateVar(null)
				.folder(FolderAPI.SYSTEM_FOLDER).host(Constants.SYSTEM_HOST).name("FormTesting" + i).owner("owner")
				.velocityVarName("velocityVarNameTesting" + i).build();
		type = factory.save(type);

		List<Field> fields = new FieldFactoryImpl().byContentTypeId(type.inode());
		List<Field> baseTypeFields = ImmutableFormContentType.builder().name("test").velocityVarName("rewarwa").build().requiredFields();
		assertThat("fields are all added", fields.size() == baseTypeFields.size());

		for (int j = 0; j < fields.size(); j++) {
			Field field = fields.get(j);
			Field baseField = baseTypeFields.get(j);
			assertThat("fields are correct:", field.dataType().equals(baseField.dataType()));
			assertThat("fields are correct:", field.variable().equals(baseField.variable()));
			assertThat("fields are correct:", field.getClass().equals(baseField.getClass()));
			assertThat("fields are correct:", field.name().equals(baseField.name()));
			assertThat("fields are correct:", field.sortOrder() == baseField.sortOrder());
		}
	}

	public void addPersona() throws DotDataException {

		long i = System.currentTimeMillis();

		ContentType type = ImmutablePersonaContentType.builder().description("description" + i).expireDateVar(null)
				.folder(FolderAPI.SYSTEM_FOLDER).host(Constants.SYSTEM_HOST).name("PersonaTesting" + i).owner("owner")
				.velocityVarName("velocityVarNameTesting" + i).build();
		type = factory.save(type);

		List<Field> fields = new FieldFactoryImpl().byContentTypeId(type.inode());
		List<Field> baseTypeFields = ContentTypeBuilder.builder(type).build().requiredFields();
		assertThat("fields are all added", fields.size() == baseTypeFields.size());

		for (int j = 0; j < fields.size(); j++) {
			Field field = fields.get(j);
			Field baseField = baseTypeFields.get(j);
			assertThat("fields dataType correct:", field.dataType().equals(baseField.dataType()));
			assertThat("fields variable correct:", field.variable().equals(baseField.variable()));
			assertThat("fields getClass correct:", field.getClass().equals(baseField.getClass()));
			assertThat("fields name are correct:", field.name().equals(baseField.name()));
			assertThat("fields sortOrder are correct:", field.sortOrder() == baseField.sortOrder());
		}
	}

	public void addFileAsset() throws DotDataException {

		long i = System.currentTimeMillis();

		ContentType type = ImmutableFileAssetContentType.builder().description("description" + i).expireDateVar(null)
				.folder(FolderAPI.SYSTEM_FOLDER).host(Constants.SYSTEM_HOST).name("FileAssetTesting" + i).owner("owner")
				.velocityVarName("velocityVarNameTesting" + i).build();
		type = factory.save(type);

		List<Field> fields = new FieldFactoryImpl().byContentTypeId(type.inode());
		List<Field> baseTypeFields = ContentTypeBuilder.builder(type).build().requiredFields();
		assertThat("fields are all added", fields.size() == baseTypeFields.size());

		for (int j = 0; j < fields.size(); j++) {
			Field field = fields.get(j);
			Field baseField = baseTypeFields.get(j);
			assertThat("fields dataType correct:", field.dataType().equals(baseField.dataType()));
			assertThat("fields variable correct:", field.variable().equals(baseField.variable()));
			assertThat("fields getClass correct:", field.getClass().equals(baseField.getClass()));
			assertThat("fields name are correct:", field.name().equals(baseField.name()));
			assertThat("fields sortOrder are correct:", field.sortOrder() == baseField.sortOrder());
		}
	}

	
	@Test
	public void searchCount() throws DotDataException {
		String query = " velocity_var_name like '%content%'";
		List<ContentType> types = factory.search(query, 10000);
		
		int count= factory.searchCount(query,BaseContentType.ANY);
		assertThat("we have content type:", types.size()>0);
		assertThat("we have the right content types:", types.size() == count);

	}
	@Test
	public void suggestVelocityVar() throws DotDataException {
		String tryVar = "content" + System.currentTimeMillis();
		String newVar = factory.suggestVelocityVar(tryVar);
		
		assertThat("random velocity var works", newVar!=null);
		assertThat("random velocity var works", newVar.equals(tryVar));
		
		
		tryVar = "news" ;
		newVar = factory.suggestVelocityVar(tryVar);
		assertThat("existing velocity var will not work", !newVar.equals(tryVar));

	}
	
	
}
