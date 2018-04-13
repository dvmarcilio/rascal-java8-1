package com.rascaljava;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

public class CompilationUnitProcessor {

	private CompilationUnit compilationUnit;
	
	private DB dbConnection;
	
	private CombinedTypeSolver solver;
	
	public CompilationUnitProcessor(String projectPath, CombinedTypeSolver solver) {
		dbConnection = DB.getInstance();
		this.solver =  solver;
	}

	public void processCompilationUnit() {
		ClassOrInterfaceDeclaration classDef = compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).get();
		if(classDef != null) {
			processClass(solver.solveType(getPackage() + "." + classDef.getName()));
		}
	}

	
	public ClassDefinition processClass(ResolvedReferenceTypeDeclaration classDec) {
		ClassDefinition classDef = processClassInformation(classDec);
		classDef.setMethods(processMethodsInformation(classDec));
		classDef.setFields(processFieldsInformation(classDec));
		dbConnection.saveToDb(classDef);
		return classDef;
	}

	public ClassDefinition processClassInformation(ResolvedReferenceTypeDeclaration clazz) {
		ClassDefinition classDef = new ClassDefinition();
		classDef.setQualifiedName(clazz.getQualifiedName());
		classDef.setClass(clazz.isClass());
		ResolvedReferenceType superClass = clazz
				.getAncestors()
				.stream()
				.filter((c) -> c.getTypeDeclaration().isClass())
				.findFirst().orElse(null);
		
		if(superClass != null) {
			ClassDefinition superClassDef = dbConnection.findByQualifiedName(superClass.getQualifiedName());
			if(superClassDef != null) {
				classDef.setSuperClass(superClassDef);
			} else {
				classDef.setSuperClass(processClass(superClass.getTypeDeclaration()));
			}
			
		}
		return classDef;
	}

	public List<MethodDefinition> processMethodsInformation(ResolvedReferenceTypeDeclaration clazz) {
//		clazz.getAllMethods()
//			.stream()
//			.map(m -> {
//				m.typeParametersMap()
//				Map<String, String> args = m.getParameters().stream().collect(Collectors.toMap((p) -> p.getNameAsString(), (p) -> p.getType().toString()));
//			})
//			.collect(Collectors.toList());
//		compilationUnit.findAll(MethodDeclaration.class).forEach((m) -> {
//			classDef.addMethodDefinition(m.getNameAsString(), m.getType().toString(), args, exceptions );
//		});
		return new ArrayList<>();
	}
	
	public List<FieldDefinition> processFieldsInformation(ResolvedReferenceTypeDeclaration clazz) {
		return clazz.getAllFields().stream().map((f) -> new FieldDefinition(f.getName(), f.getType().toString())).collect(Collectors.toList());
	}

	public String getPackage() {
		return compilationUnit.getPackageDeclaration().get().getNameAsString();
	}

	public static CompilationUnit getCompilationUnit(File file) {
		try {
			return JavaParser.parse(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	public void setCompilationUnit(CompilationUnit compilationUnit) {
		this.compilationUnit = compilationUnit;
	}
}
