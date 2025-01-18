use std::collections::BTreeMap;

use crate::core::config::{Config, Type};
use crate::core::transform::Transform;
use tailcall_valid::Valid;

pub struct CheckMethodType;

impl Transform for CheckMethodType {
    type Value = Config;
    type Error = String;
    fn transform(&self, mut config: Config) -> Valid<Self::Value, Self::Error> {
        let query_name = config.schema.query.clone().unwrap_or(String::from("Query"));
        let mutation_name = String::from("Mutation");

        let query = config.types.remove(&query_name);

        if let Some(mut query) = query {
            let (query_fields, mutation_fields) = query
                .fields
                .into_iter()
                .partition::<BTreeMap<_, _>, _>(|(k, _)| check_is_query_by_method_name(k));

            query.fields = query_fields;

            if !mutation_fields.is_empty() {
                let mut mutation = config
                    .types
                    .remove(&mutation_name)
                    .unwrap_or(Type::default());

                mutation.fields.extend(mutation_fields);

                config.types.insert(mutation_name.clone(), mutation);

                config.schema.mutation = Some(mutation_name);
            }

            if !query.fields.is_empty() {
                config.types.insert(query_name, query);
            }
        }

        Valid::succeed(config)
    }
}

fn check_is_query_by_method_name(method_name: &str) -> bool {
    ["detail", "get", "list"]
        .iter()
        .any(|k| method_name.starts_with(k))
}
