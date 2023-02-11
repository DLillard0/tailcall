package tailcall.gateway.remote
import zio.schema.{DynamicValue, Schema}

trait UnsafeEvaluator  {
  final def evaluateAs[A](eval: DynamicEval): A = evaluate(eval).asInstanceOf[A]
  def evaluate(eval: DynamicEval): Any
}
object UnsafeEvaluator {
  import DynamicEval._
  import scala.collection.mutable

  final class Default(val bindings: mutable.Map[Int, Any]) extends UnsafeEvaluator {
    import UnsafeEvaluator.Error._

    private def toTypedValue(value: DynamicValue, schema: Schema[_]): Any = {
      value.toTypedValue(schema) match {
        case Left(cause)  => throw TypeError(value, cause, schema)
        case Right(value) => value
      }
    }

    def evaluate(eval: DynamicEval): Any =
      eval match {
        case Literal(value, meta)        => toTypedValue(value, meta.toSchema)
        case EqualTo(left, right, tag)   => tag.equal(evaluate(left), evaluate(right))
        case Math(operation, tag)        => operation match {
            case Math.Binary(left, right, operation) =>
              val leftValue  = evaluate(left)
              val rightValue = evaluate(right)
              operation match {
                case Math.Binary.Add      => tag.add(leftValue, rightValue)
                case Math.Binary.Multiply => tag.multiply(leftValue, rightValue)
                case Math.Binary.Divide   => tag.divide(leftValue, rightValue)
                case Math.Binary.Modulo   => tag.modulo(leftValue, rightValue)
              }
            case Math.Unary(value, operation)        =>
              val a = evaluate(value)
              operation match { case Math.Unary.Negate => tag.negate(a) }
          }
        case Logical(operation)          => operation match {
            case Logical.Binary(left, right, operation) =>
              val leftValue  = evaluateAs[Boolean](left)
              val rightValue = evaluateAs[Boolean](right)
              operation match {
                case Logical.Binary.And => leftValue && rightValue
                case Logical.Binary.Or  => leftValue || rightValue
              }
            case Logical.Unary(value, operation)        =>
              val a = evaluateAs[Boolean](value)
              operation match {
                case Logical.Unary.Not                      => !a
                case Logical.Unary.Diverge(isTrue, isFalse) =>
                  if (a) evaluate(isTrue) else evaluate(isFalse)
              }
          }
        case StringOperations(operation) => operation match {
            case StringOperations.Concat(left, right) =>
              evaluateAs[String](left) ++ evaluateAs[String](right)
          }
        case SeqOperations(operation)    => operation match {
            case SeqOperations.Concat(left, right)    =>
              evaluateAs[Seq[_]](left) ++ evaluateAs[Seq[_]](right)
            case SeqOperations.IndexOf(seq, element)  =>
              evaluateAs[Seq[_]](seq).indexOf(evaluate(element))
            case SeqOperations.Reverse(seq)           => evaluateAs[Seq[_]](seq).reverse
            case SeqOperations.Filter(seq, condition) =>
              evaluateAs[Seq[_]](seq).filter(call[Boolean](condition, _))

            case SeqOperations.FlatMap(seq, operation) =>
              evaluateAs[Seq[_]](seq).flatMap(call[Seq[_]](operation, _))
            case SeqOperations.Length(seq)             => evaluateAs[Seq[_]](seq).length
            case SeqOperations.Sequence(value)         => value.map(evaluate(_))
          }
        case EitherOperations(operation) => operation match {
            case EitherOperations.Cons(value)              => value match {
                case Left(value)  => Left(evaluate(value))
                case Right(value) => Right(evaluate(value))
              }
            case EitherOperations.Fold(value, left, right) => evaluate(value) match {
                case Left(value)  => call(left, value)
                case Right(value) => call(right, value)
              }
          }
        case FunctionCall(f, arg)        => call(f, evaluate(arg))
        case Binding(id)                 => bindings.getOrElse(id, throw BindingNotFound(id))
        case EvalFunction(_, body)       => evaluate(body)
        case OptionOperations(operation) => operation match {
            case OptionOperations.Cons(option)            => option match {
                case Some(value) => Some(evaluate(value))
                case None        => None
              }
            case OptionOperations.Fold(value, none, some) => evaluate(value) match {
                case Some(value) => call(some, value)
                case None        => evaluate(none)
              }
          }

        case ContextOperations(self, operation) => ???
        case Die(message)                       => throw Error.Died(evaluateAs[String](message))
      }

    def call[A](func: EvalFunction, arg: Any): A = {
      bindings.addOne(func.input.id -> arg)
      val result = evaluateAs[A](func.body)
      bindings.drop(func.input.id)
      result
    }
  }

  def make(bindings: mutable.Map[Int, Any] = mutable.Map.empty): UnsafeEvaluator =
    new Default(bindings)

  sealed trait Error extends Throwable {
    self =>
    override def getMessage(): String = Error.getMessage(self)
  }

  object Error {
    final case class FieldNotFound(name: String)                                      extends Error
    final case class UnsupportedOperation(operation: String, value: DynamicValue)     extends Error
    final case class TypeError(value: DynamicValue, cause: String, schema: Schema[_]) extends Error
    final case class BindingNotFound(id: Int)                                         extends Error

    final case class Died(message: String) extends Error

    def getMessage(self: Error): String =
      self match {
        case FieldNotFound(name)                    => s"Field not found: $name"
        case UnsupportedOperation(operation, value) =>
          s"Unsupported operation: $operation on $value"
        case TypeError(value, cause, schema) => s"Type conversion error: $value, $cause, $schema"
        case BindingNotFound(id)             => s"Binding not found: $id"
        case Died(message)                   => s"Died because of: $message"
      }
  }
}
