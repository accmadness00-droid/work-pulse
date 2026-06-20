import type { Rule } from "antd/es/form";

export const PHONE_NUMBER_PLACEHOLDER = "+998901234567";

const PHONE_NUMBER_PATTERN = /^\+[1-9]\d{7,14}$/;

export function phoneNumberRules(): Rule[] {
  return [
    {
      pattern: PHONE_NUMBER_PATTERN,
      message: `Enter a valid international phone number, for example ${PHONE_NUMBER_PLACEHOLDER}`
    }
  ];
}
