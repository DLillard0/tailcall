use crate::core::config::{config, Config, Type};
use crate::core::transform::Transform;
use tailcall_valid::Valid;

pub struct HandleNamespaces;

impl Transform for HandleNamespaces {
    type Value = Config;
    type Error = String;
    fn transform(&self, mut config: Config) -> Valid<Self::Value, Self::Error> {
        let query_name = config.schema.query.clone().unwrap_or(String::from("Query"));
        let mutation_name = config.schema.mutation.clone().unwrap_or(String::from("Mutation"));
        let namespace = config.namespace.clone().unwrap_or("".to_string());

        if namespace.is_empty() {
            return Valid::succeed(config);
        }

        // 给当前 query 修改 fields 增加 namespace types
        let query_type = config.types.entry(query_name.clone()).or_insert(Type::default());
        let namespace_type = query_type.clone();

        // 移除 query_name
        config.types.remove(&query_name.to_string());

        let mut new_field = config::Field::default();
        new_field.type_of = crate::core::Type::from(namespace.clone() + "Query");

        let mut new_query_type = config::Type::default();
        new_query_type.fields.insert(namespace.clone(), new_field);

        config.types.insert(query_name, new_query_type);
        config.types.insert(namespace.clone() + "Query", namespace_type);

        // 给当前 mutation 修改 fields 增加 namespace types
        let mutation_type = config.types.entry(mutation_name.clone()).or_insert(Type::default());
        let namespace_type = mutation_type.clone();

        // 移除 mutation_name
        config.types.remove(&mutation_name.to_string());

        let mut new_field = config::Field::default();
        new_field.type_of = crate::core::Type::from(namespace.clone() + "Mutation");

        let mut new_mutation_type = config::Type::default();
        new_mutation_type.fields.insert(namespace.clone(), new_field);

        config.types.insert(mutation_name, new_mutation_type);
        config.types.insert(namespace.clone() + "Mutation", namespace_type);

        Valid::succeed(config)
    }
}
