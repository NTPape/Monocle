package monocle.internal.focus

import scala.quoted.Type

private[focus] trait LambdaConfigParser {
  this: FocusBase => 

  import macroContext.reflect._
  
  def parseLambdaConfig[From: Type](lambda: Term): FocusResult[LambdaConfig] = {
    val fromTypeIsConcrete = TypeRepr.of[From].classSymbol.isDefined

    lambda match {
      case WithMagicKeywords(ExpectedLambdaFunction(config)) if fromTypeIsConcrete => Right(config)
      case WithMagicKeywords(ExpectedLambdaFunction(_)) => FocusError.NotAConcreteClass(Type.show[Type[From]]).asResult 
      case WithMagicKeywords(_) => FocusError.NotASimpleLambdaFunction.asResult
      case _ => FocusError.CouldntRemoveMagicKeywords.asResult
    }
  }

  private object WithMagicKeywords {
    def unapply(lambdaWithMagic: Term): Option[Term] = unwrap(lambdaWithMagic) match {
      case Block(List(DefDef(_, _, _, _, Some(magicFreeLambda))), _) => Some(magicFreeLambda)
      case _ => None
    }
  }
    
  private def unwrap(term: Term): Term = {
    term match {
      case Block(List(), inner) => unwrap(inner)
      case Inlined(_, _, inner) => unwrap(inner)
      case x => x
    }
  }

  private object ExpectedLambdaFunction {
    def unapply(term: Term): Option[LambdaConfig] = 
      unwrap(term) match {
        case Lambda(List(ValDef(argName, _, _)), body) => Some(LambdaConfig(argName, body))
        case _ => None
      }
  }
}