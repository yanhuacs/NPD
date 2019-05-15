package stringanalysis;
import org.junit.Test;

public class SATest {	
	
	@Test
	public void ArrayAssignment(){
		String mainclass="stringanalysis.testee.ArrayAssignment";
		testSA(mainclass);
	}
	@Test
	public void ArrayClone(){
		String mainclass="stringanalysis.testee.ArrayClone";
		testSA(mainclass);
	}
	@Test
	public void ArrayUpdate(){
		String mainclass="stringanalysis.testee.ArrayUpdate";
		testSA(mainclass);
	}
	@Test
	public void BasicMethodCall(){
		String mainclass="stringanalysis.testee.BasicMethodCall";
		testSA(mainclass);
	}
	@Test
	public void CharArgument(){
		String mainclass="stringanalysis.testee.CharArgument";
		testSA(mainclass);
	}	
	@Test
	public void CharAtIntegerString(){
		String mainclass="stringanalysis.testee.CharAtIntegerString";
		testSA(mainclass);
	}
	@Test
	public void CharCast(){
		String mainclass="stringanalysis.testee.CharCast";
		testSA(mainclass);
	}
	@Test
	public void ClassWithToString(){
		String mainclass="stringanalysis.testee.ClassWithToString";
		testSA(mainclass);
	}
	@Test
	public void ConcatBranch(){
		String mainclass="stringanalysis.testee.ConcatBranch";
		testSA(mainclass);
	}
	@Test
	public void ConcatInterprocedual(){
		String mainclass="stringanalysis.testee.ConcatInterprocedual";
		testSA(mainclass);
	}
	@Test
	public void ContainVariable(){
		String mainclass="stringanalysis.testee.ContainVariable";
		testSA(mainclass);
	}
	@Test
	public void ExpreSensitivity(){
		String mainclass="stringanalysis.testee.ExpreSensitivity";
		testSA(mainclass);
	}
	@Test
	public void FieldCallMethod(){
		String mainclass="stringanalysis.testee.FieldCallMethod";
		testSA(mainclass);
	}
	@Test
	public void FieldConditional(){
		String mainclass="stringanalysis.testee.FieldConditional";
		testSA(mainclass);
	}
	@Test
	public void FieldNested(){
		String mainclass="stringanalysis.testee.FieldNested";
		testSA(mainclass);
	}
	@Test
	public void FieldSetOnOther(){
		String mainclass="stringanalysis.testee.FieldSetOnOther";
		testSA(mainclass);
	}
	@Test
	public void FieldStaticDefineByOtherFunction(){
		String mainclass="stringanalysis.testee.FieldStaticDefineByOtherFunction";
		testSA(mainclass);
	}
	@Test
	public void IfCompoundContain(){
		String mainclass="stringanalysis.testee.IfCompoundContain";
		testSA(mainclass);
	}
	@Test
	public void IfEndwith(){
		String mainclass="stringanalysis.testee.IfEndwith";
		testSA(mainclass);
	}
	@Test
	public void IfEqualsNotString(){
		String mainclass="stringanalysis.testee.IfEqualsNotString";
		testSA(mainclass);
	}
	@Test
	public void IfLengthEqualsConst(){
		String mainclass="stringanalysis.testee.IfLengthEqualsConst";
		testSA(mainclass);
	}
	@Test
	public void IfStartwith(){
		String mainclass="stringanalysis.testee.IfStartwith";
		testSA(mainclass);
	}
	@Test
	public void InterfaceMethod(){
		String mainclass="stringanalysis.testee.InterfaceMethod";
		testSA(mainclass);
	}
	@Test
	public void IrrelevantArgs(){
		String mainclass="stringanalysis.testee.IrrelevantArgs";
		testSA(mainclass);
	}
	@Test
	public void MethodCall(){
		String mainclass="stringanalysis.testee.MethodCall";
		testSA(mainclass);
	}
	@Test
	public void ReplaceCalls(){
		String mainclass="stringanalysis.testee.ReplaceCalls";
		testSA(mainclass);
	}
	@Test
	public void ReplaceSubwithSuper(){
		String mainclass="stringanalysis.testee.ReplaceSubwithSuper";
		testSA(mainclass);
	}
	@Test
	public void ReplaceSuperwithSub(){
		String mainclass="stringanalysis.testee.ReplaceSuperwithSub";
		testSA(mainclass);
	}
	@Test
	public void StringBufferNull(){
		String mainclass="stringanalysis.testee.StringBufferNull";
		testSA(mainclass);
	}
	@Test
	public void StringOperations(){
		String mainclass="stringanalysis.testee.StringOperations";
		testSA(mainclass);
	}
	@Test
	public void StringConstructor(){
		String mainclass="stringanalysis.testee.StringConstructor";
		testSA(mainclass);
	}
	@Test
	public void SubstringOfNull(){
		String mainclass="stringanalysis.testee.SubstringOfNull";
		testSA(mainclass);
	}
	@Test
	public void SwitchChar(){
		String mainclass="stringanalysis.testee.SwitchChar";
		testSA(mainclass);
	}
	@Test
	public void ValueOfObject(){
		String mainclass="stringanalysis.testee.ValueOfObject";
		testSA(mainclass);
	}
	
	public void testSA(String mainclass){
		String[] args=new String[]{
				"-jre",
				"jre/jre1.6.0_45",
				"-apppath", // app path
				"testclasses",//app path
				"-mainclass", // specify main class
				mainclass, // main class
				"-reflection",
				"-kobjsens",
				"0",
				"-stringanalysis",
		};
		driver.Main.main(args);
	}
}
